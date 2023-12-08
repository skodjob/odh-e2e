/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e.upgrade;

import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.odh.test.Environment;
import io.odh.test.OdhAnnotationsLabels;
import io.odh.test.OdhConstants;
import io.odh.test.TestConstants;
import io.odh.test.e2e.Abstract;
import io.odh.test.framework.listeners.OdhResourceCleaner;
import io.odh.test.framework.listeners.ResourceManagerDeleteHandler;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.framework.manager.resources.NotebookResource;
import io.odh.test.install.BundleInstall;
import io.odh.test.utils.DeploymentUtils;
import io.odh.test.utils.PodUtils;
import io.opendatahub.datasciencecluster.v1.DataScienceCluster;
import io.opendatahub.datasciencecluster.v1.DataScienceClusterBuilder;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.ComponentsBuilder;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Codeflare;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.CodeflareBuilder;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Dashboard;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.DashboardBuilder;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Datasciencepipelines;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.DatasciencepipelinesBuilder;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Kserve;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.KserveBuilder;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Workbenches;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.WorkbenchesBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kubeflow.v1.Notebook;
import org.kubeflow.v1.NotebookBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsNot.not;

@Tag("upgrade")
@ExtendWith(OdhResourceCleaner.class)
@ExtendWith(ResourceManagerDeleteHandler.class)
public class BundleUpgradeST extends Abstract {

    private static final Logger LOGGER = LoggerFactory.getLogger(BundleUpgradeST.class);

    BundleInstall baseBundle;
    BundleInstall upgradeBundle;

    @AfterAll
    void clean() throws IOException {
        if (upgradeBundle != null) {
            upgradeBundle.deleteWithoutResourceManager();
        }
    }

    @Test
    void testUpgradeBundle() throws IOException {
        LOGGER.info("Install base version");
        baseBundle = new BundleInstall(Environment.INSTALL_FILE_PREVIOUS_PATH);
        baseBundle.createWithoutResourceManager();

        Map<String, String> operatorSnapshot = DeploymentUtils.depSnapshot(baseBundle.getNamespace(), baseBundle.getDeploymentName());

        String dsProjectName = "test-notebooks-upgrade";
        String ntbName = "test-odh-notebook";
        String ntbNamespace = "test-odh-notebook-upgrade";

        DataScienceCluster dsc = new DataScienceClusterBuilder()
                .withNewMetadata()
                .withName(dsProjectName)
                .endMetadata()
                .withNewSpec()
                .withComponents(
                        new ComponentsBuilder()
                                .withWorkbenches(
                                        new WorkbenchesBuilder().withManagementState(Workbenches.ManagementState.MANAGED).build()
                                )
                                .withDashboard(
                                        new DashboardBuilder().withManagementState(Dashboard.ManagementState.MANAGED).build()
                                )
                                .withKserve(
                                        new KserveBuilder().withManagementState(Kserve.ManagementState.REMOVED).build()
                                )
                                .withCodeflare(
                                        new CodeflareBuilder().withManagementState(Codeflare.ManagementState.REMOVED).build()
                                )
                                .withDatasciencepipelines(
                                        new DatasciencepipelinesBuilder().withManagementState(Datasciencepipelines.ManagementState.REMOVED).build()
                                )
                                .build())
                .endSpec()
                .build();
        // Deploy DSC
        ResourceManager.getInstance().createResourceWithWait(dsc);

        Namespace ns = new NamespaceBuilder()
                .withNewMetadata()
                .withName(ntbNamespace)
                .addToLabels(OdhAnnotationsLabels.LABEL_DASHBOARD, "true")
                .addToAnnotations(OdhAnnotationsLabels.ANNO_SERVICE_MESH, "false")
                .endMetadata()
                .build();
        ResourceManager.getInstance().createResourceWithoutWait(ns);

        PersistentVolumeClaim pvc = new PersistentVolumeClaimBuilder()
                .withNewMetadata()
                .withName(ntbName)
                .withNamespace(ntbNamespace)
                .addToLabels(OdhAnnotationsLabels.LABEL_DASHBOARD, "true")
                .endMetadata()
                .withNewSpec()
                .addToAccessModes("ReadWriteOnce")
                .withNewResources()
                .addToRequests("storage", new Quantity("10Gi"))
                .endResources()
                .withVolumeMode("Filesystem")
                .endSpec()
                .build();
        ResourceManager.getInstance().createResourceWithoutWait(pvc);

        Notebook notebook = new NotebookBuilder(NotebookResource.loadDefaultNotebook(ntbNamespace, ntbName)).build();
        ResourceManager.getInstance().createResourceWithoutWait(notebook);

        LabelSelector lblSelector = new LabelSelectorBuilder()
                .withMatchLabels(Map.of("app", ntbName))
                .build();

        PodUtils.waitForPodsReady(ntbNamespace, lblSelector, 1, true, () -> { });

        LOGGER.info("Upgrade to latest version");
        upgradeBundle = new BundleInstall(Environment.INSTALL_FILE_PATH);
        upgradeBundle.createWithoutResourceManager();

        DeploymentUtils.waitTillDepHasRolled(baseBundle.getNamespace(), baseBundle.getDeploymentName(), operatorSnapshot);

        LabelSelector labelSelector = ResourceManager.getClient().getDeployment(TestConstants.ODH_NAMESPACE, OdhConstants.ODH_DASHBOARD).getSpec().getSelector();
        PodUtils.verifyThatPodsAreStable(TestConstants.ODH_NAMESPACE, labelSelector);

        // Check that operator doesn't contain errors in logs
        String operatorLog = ResourceManager.getClient().getClient().apps().deployments()
                .inNamespace(baseBundle.getNamespace()).withName(baseBundle.getDeploymentName()).getLog();

        assertThat(operatorLog, not(containsString("error")));
        assertThat(operatorLog, not(containsString("Error")));
        assertThat(operatorLog, not(containsString("ERROR")));
    }
}
