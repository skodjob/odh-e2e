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
import io.odh.test.Environment;
import io.odh.test.OdhAnnotationsLabels;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.framework.manager.resources.NotebookResource;
import io.odh.test.utils.DscUtils;
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
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Ray;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.RayBuilder;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Workbenches;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.WorkbenchesBuilder;
import io.opendatahub.dscinitialization.v1.DSCInitialization;
import io.skodjob.annotations.Contact;
import io.skodjob.annotations.Desc;
import io.skodjob.annotations.Step;
import io.skodjob.annotations.SuiteDoc;
import io.skodjob.annotations.TestDoc;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kubeflow.v1.Notebook;
import org.kubeflow.v1.NotebookBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

@SuiteDoc(
    description = @Desc("Verifies deployments of Notebooks via GitOps approach"),
    beforeTestSteps = {
        @Step(value = "Deploy Pipelines Operator", expected = "Pipelines operator is available on the cluster"),
        @Step(value = "Deploy ServiceMesh Operator", expected = "ServiceMesh operator is available on the cluster"),
        @Step(value = "Deploy Serverless Operator", expected = "Serverless operator is available on the cluster"),
        @Step(value = "Install ODH operator", expected = "Operator is up and running and is able to serve it's operands"),
        @Step(value = "Deploy DSCI", expected = "DSCI is created and ready"),
        @Step(value = "Deploy DSC", expected = "DSC is created and ready")
    },
    afterTestSteps = {
        @Step(value = "Delete ODH operator and all created resources", expected = "Operator is removed and all other resources as well")
    }
)
public class NotebookST extends StandardAbstract {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotebookST.class);

    private static final String DS_PROJECT_NAME = "test-notebooks";

    private static final String NTB_NAME = "test-odh-notebook";
    private static final String NTB_NAMESPACE = "test-odh-notebook";

    @TestDoc(
        description = @Desc("Create simple Notebook with all needed resources and see if Operator creates it properly"),
        contact = @Contact(name = "Jakub Stejskal", email = "jstejska@redhat.com"),
        steps = {
            @Step(value = "Create namespace for Notebook resources with proper name, labels and annotations", expected = "Namespace is created"),
            @Step(value = "Create PVC with proper labels and data for Notebook", expected = "PVC is created"),
            @Step(value = "Create Notebook resource with Pytorch image in pre-defined namespace", expected = "Notebook resource is created"),
            @Step(value = "Wait for Notebook pods readiness", expected = "Notebook pods are up and running, Notebook is in ready state")
        }
    )
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

        String notebookImage = NotebookResource.getNotebookImage(NotebookResource.JUPYTER_MINIMAL_IMAGE, NotebookResource.JUPYTER_MINIMAL_2023_2_TAG);
        Notebook notebook = new NotebookBuilder(NotebookResource.loadDefaultNotebook(NTB_NAMESPACE, NTB_NAME, notebookImage)).build();
        ResourceManager.getInstance().createResourceWithoutWait(notebook);

        LabelSelector lblSelector = new LabelSelectorBuilder()
                .withMatchLabels(Map.of("app", NTB_NAME))
                .build();

        PodUtils.waitForPodsReady(NTB_NAMESPACE, lblSelector, 1, true, () -> { });
    }

    @BeforeAll
    void deployDataScienceCluster() {
        if (Environment.SKIP_DEPLOY_DSCI_DSC) {
            LOGGER.info("DSCI and DSC deploy is skipped");
            return;
        }
        // Create DSCI
        DSCInitialization dsci = DscUtils.getBasicDSCI();

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
                            new KserveBuilder().withManagementState(Kserve.ManagementState.MANAGED).build()
                        )
                        .withCodeflare(
                            new CodeflareBuilder().withManagementState(Codeflare.ManagementState.MANAGED).build()
                        )
                        .withDatasciencepipelines(
                            new DatasciencepipelinesBuilder().withManagementState(Datasciencepipelines.ManagementState.MANAGED).build()
                        )
                        .withModelmeshserving(
                            new ModelmeshservingBuilder().withManagementState(Modelmeshserving.ManagementState.MANAGED).build()
                        )
                        .withRay(
                            new RayBuilder().withManagementState(Ray.ManagementState.MANAGED).build()
                        )
                        .build())
                .endSpec()
                .build();
        // Deploy DSCI,DSC
        ResourceManager.getInstance().createResourceWithWait(dsci);
        ResourceManager.getInstance().createResourceWithWait(dsc);
    }
}
