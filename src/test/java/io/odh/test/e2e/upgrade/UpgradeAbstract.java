/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e.upgrade;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.odh.test.OdhAnnotationsLabels;
import io.odh.test.TestSuite;
import io.odh.test.e2e.Abstract;
import io.odh.test.framework.manager.resources.NotebookType;
import io.odh.test.utils.DscUtils;
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
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Kueue;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.KueueBuilder;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Modelmeshserving;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.ModelmeshservingBuilder;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Ray;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.RayBuilder;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Workbenches;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.WorkbenchesBuilder;
import io.opendatahub.dscinitialization.v1.DSCInitialization;
import io.skodjob.testframe.annotations.ResourceManager;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.junit.jupiter.api.Tag;
import org.kubeflow.v1.Notebook;
import org.kubeflow.v1.NotebookBuilder;

import java.io.IOException;

@Tag(TestSuite.UPGRADE)
@ResourceManager
public abstract class UpgradeAbstract extends Abstract {

    protected void deployDsc(String name) {
        DSCInitialization dsci = DscUtils.getBasicDSCI();

        // Deploy DSC
        DataScienceCluster dsc = new DataScienceClusterBuilder()
            .withNewMetadata()
            .withName(name)
            .endMetadata()
            .withNewSpec()
            .withComponents(
                new ComponentsBuilder()
                    .withWorkbenches(
                        new WorkbenchesBuilder().withManagementState(Workbenches.ManagementState.Managed).build()
                    )
                    .withDashboard(
                        new DashboardBuilder().withManagementState(Dashboard.ManagementState.Managed).build()
                    )
                    .withKserve(
                        new KserveBuilder().withManagementState(Kserve.ManagementState.Removed).build()
                    )
                    .withKueue(
                        new KueueBuilder().withManagementState(Kueue.ManagementState.Managed).build()
                    )
                    .withCodeflare(
                        new CodeflareBuilder().withManagementState(Codeflare.ManagementState.Managed).build()
                    )
                    .withDatasciencepipelines(
                        new DatasciencepipelinesBuilder().withManagementState(Datasciencepipelines.ManagementState.Managed).build()
                    )
                    .withModelmeshserving(
                        new ModelmeshservingBuilder().withManagementState(Modelmeshserving.ManagementState.Managed).build()
                    )
                    .withRay(
                        new RayBuilder().withManagementState(Ray.ManagementState.Managed).build()
                    )
                    .build())
            .endSpec()
            .build();
        // Deploy DSC
        KubeResourceManager.getInstance().createOrUpdateResourceWithWait(dsci);
        KubeResourceManager.getInstance().createResourceWithWait(dsc);
    }
    public void deployNotebook(String namespace, String name) throws IOException {
        PersistentVolumeClaim pvc = new PersistentVolumeClaimBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
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
        KubeResourceManager.getInstance().createResourceWithoutWait(pvc);

        String notebookImage = NotebookType.getNotebookImage(NotebookType.JUPYTER_MINIMAL_IMAGE, NotebookType.JUPYTER_MINIMAL_2023_2_TAG);
        Notebook notebook = new NotebookBuilder(NotebookType.loadDefaultNotebook(namespace, name, notebookImage)).build();

        KubeResourceManager.getInstance().createResourceWithoutWait(notebook);
    }
}
