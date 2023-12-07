/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e.upgrade;

import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.InstallPlan;
import io.odh.test.OdhConstants;
import io.odh.test.TestConstants;
import io.odh.test.TestUtils;
import io.odh.test.e2e.Abstract;
import io.odh.test.framework.listeners.OdhResourceCleaner;
import io.odh.test.framework.listeners.ResourceManagerDeleteHandler;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.install.OlmInstall;
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
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Modelmeshserving;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.ModelmeshservingBuilder;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Workbenches;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.WorkbenchesBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsNot.not;

import java.util.Map;
import java.util.NoSuchElementException;

@Tag("upgrade")
@ExtendWith(OdhResourceCleaner.class)
@ExtendWith(ResourceManagerDeleteHandler.class)
public class OlmUpgradeST extends Abstract {

    private static final Logger LOGGER = LoggerFactory.getLogger(OlmUpgradeST.class);
    private static final String DS_PROJECT_NAME = "upgrade-dsc";

    private final String startingVersion = "2.3.0";

    @Test
    void testUpgradeOlm() {
        OlmInstall olmInstall = new OlmInstall();
        olmInstall.setApproval("Manual");
        olmInstall.setStartingCsv(olmInstall.getOperatorName() + ".v" + startingVersion);
        olmInstall.createManual();

        // Approve install plan created for older version
        InstallPlan ip = ResourceManager.getClient().getNonApprovedInstallPlan(olmInstall.getNamespace(), olmInstall.getOperatorName());
        ResourceManager.getClient().approveInstallPlan(olmInstall.getNamespace(), ip.getMetadata().getName());
        // Wait for old version readiness
        DeploymentUtils.waitForDeploymentReady(olmInstall.getNamespace(), olmInstall.getDeploymentName());

        // Make snapshot of current operator
        Map<String, String> operatorSnapshot = DeploymentUtils.depSnapshot(olmInstall.getNamespace(), olmInstall.getDeploymentName());

        // Deploy DSC
        DataScienceCluster dsc = new DataScienceClusterBuilder()
            .withNewMetadata()
            .withName(DS_PROJECT_NAME)
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
                            new CodeflareBuilder().withManagementState(Codeflare.ManagementState.MANAGED).build()
                    )
                    .withDatasciencepipelines(
                            new DatasciencepipelinesBuilder().withManagementState(Datasciencepipelines.ManagementState.MANAGED).build()
                    )
                    .withModelmeshserving(
                            new ModelmeshservingBuilder().withManagementState(Modelmeshserving.ManagementState.REMOVED).build()
                    )
                    .build())
            .endSpec()
            .build();
        // Deploy DSC
        ResourceManager.getInstance().createResourceWithWait(dsc);

        // Approve upgrade to newer version
        // TODO - add dynamic wait
        TestUtils.waitFor("Install paln with new version", TestConstants.GLOBAL_POLL_INTERVAL_SHORT, TestConstants.GLOBAL_TIMEOUT, () -> {
            try {
                ResourceManager.getClient().getNonApprovedInstallPlan(olmInstall.getNamespace(), olmInstall.getCsvName());
                return true;
            } catch (NoSuchElementException ex) {
                LOGGER.debug("No new install plan available. Checking again ...");
                return false;
            }
        }, () -> { });

        ip = ResourceManager.getClient().getNonApprovedInstallPlan(olmInstall.getNamespace(), olmInstall.getCsvName());
        ResourceManager.getClient().approveInstallPlan(olmInstall.getNamespace(), ip.getMetadata().getName());
        // Wait for operator RU
        DeploymentUtils.waitTillDepHasRolled(olmInstall.getNamespace(), olmInstall.getDeploymentName(), operatorSnapshot);

        // Wait for pod stability for Dashboard
        LabelSelector labelSelector = ResourceManager.getClient().getDeployment(TestConstants.ODH_NAMESPACE, OdhConstants.ODH_DASHBOARD).getSpec().getSelector();
        PodUtils.verifyThatPodsAreStable(TestConstants.ODH_NAMESPACE, labelSelector);

        // Check that operator doesn't contains errors in logs
        String operatorLog = ResourceManager.getClient().getClient().apps().deployments()
                .inNamespace(olmInstall.getNamespace()).withName(olmInstall.getDeploymentName()).getLog();

        assertThat(operatorLog, not(containsString("error")));
        assertThat(operatorLog, not(containsString("Error")));
        assertThat(operatorLog, not(containsString("ERROR")));
    }
}
