/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e.standard;

import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.odh.test.OdhAnnotationsLabels;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.framework.manager.resources.NotebookResource;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kubeflow.v1.Notebook;
import org.kubeflow.v1.NotebookBuilder;

import java.io.IOException;
import java.util.Map;

public class NotebookST extends StandardAbstract {

    private static final String DS_PROJECT_NAME = "test-notebooks";

    private static final String NTB_NAME = "test-odh-notebook";
    private static final String NTB_NAMESPACE = "test-odh-notebook";
    @Test
    void testCreateSimpleNotebook() throws IOException {
        // Create namespace
        Namespace ns = new NamespaceBuilder()
            .withNewMetadata()
            .withName(NTB_NAMESPACE)
            .addToLabels(OdhAnnotationsLabels.LABEL_DASHBOARD, "true")
            .addToAnnotations(OdhAnnotationsLabels.ANNO_SERVICE_MESH, "false")
            .endMetadata()
            .build();
        ResourceManager.getInstance().createResourceWithoutWait(ns);

        PersistentVolumeClaim pvc = new PersistentVolumeClaimBuilder()
                .withNewMetadata()
                .withName(NTB_NAME)
                .withNamespace(NTB_NAMESPACE)
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

        Notebook notebook = new NotebookBuilder(NotebookResource.loadDefaultNotebook(NTB_NAMESPACE, NTB_NAME)).build();
        ResourceManager.getInstance().createResourceWithoutWait(notebook);

        LabelSelector lblSelector = new LabelSelectorBuilder()
                .withMatchLabels(Map.of("app", NTB_NAME))
                .build();

        PodUtils.waitForPodsReady(NTB_NAMESPACE, lblSelector, 1, true, () -> { });

    }

    @BeforeAll
    void deployDataScienceCluster() {
        // Create DSC
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
    }
}
