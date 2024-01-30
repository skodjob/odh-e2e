/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e.standard;

import io.odh.test.OdhConstants;
import io.odh.test.TestSuite;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.framework.manager.requirements.ServiceMeshOperator;
import io.odh.test.framework.manager.resources.DataScienceClusterResource;
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
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Trustyai;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.TrustyaiBuilder;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Workbenches;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.WorkbenchesBuilder;
import io.opendatahub.dscinitialization.v1.DSCInitialization;
import io.opendatahub.dscinitialization.v1.DSCInitializationBuilder;
import io.opendatahub.dscinitialization.v1.dscinitializationspec.Monitoring;
import io.opendatahub.dscinitialization.v1.dscinitializationspec.ServiceMesh;
import io.opendatahub.dscinitialization.v1.dscinitializationspec.servicemesh.ControlPlane;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(TestSuite.SMOKE)
public class DataScienceClusterST extends StandardAbstract {

    private static final String DS_PROJECT_NAME = "test-dsp";

    @Test
    void createDataScienceCluster() {
        DSCInitialization dsci = new DSCInitializationBuilder()
                .withNewMetadata()
                .withName(OdhConstants.DEFAULT_DSCI_NAME)
                .endMetadata()
                .withNewSpec()
                .withApplicationsNamespace(OdhConstants.CONTROLLERS_NAMESPACE)
                .withNewMonitoring()
                .withManagementState(Monitoring.ManagementState.MANAGED)
                .withNamespace(OdhConstants.MONITORING_NAMESPACE)
                .endMonitoring()
                .withNewServiceMesh()
                .withManagementState(ServiceMesh.ManagementState.MANAGED)
                .withNewControlPlane()
                .withName(ServiceMeshOperator.SERVICE_MESH_NAME)
                .withNamespace(ServiceMeshOperator.SERVICE_MESH_NAMESPACE)
                .withMetricsCollection(ControlPlane.MetricsCollection.ISTIO)
                .endControlPlane()
                .endServiceMesh()
                .endSpec()
                .build();

        DataScienceCluster c = new DataScienceClusterBuilder()
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
                                .withTrustyai(
                                        new TrustyaiBuilder().withManagementState(Trustyai.ManagementState.MANAGED).build()
                                )
                                .build())
                .endSpec()
                .build();

        ResourceManager.getInstance().createResourceWithWait(dsci);
        ResourceManager.getInstance().createResourceWithWait(c);

        DataScienceCluster cluster = DataScienceClusterResource.dataScienceCLusterClient().withName(DS_PROJECT_NAME).get();

        assertEquals(Kserve.ManagementState.MANAGED, cluster.getSpec().getComponents().getKserve().getManagementState());
        assertEquals(Codeflare.ManagementState.MANAGED, cluster.getSpec().getComponents().getCodeflare().getManagementState());
        assertEquals(Dashboard.ManagementState.MANAGED, cluster.getSpec().getComponents().getDashboard().getManagementState());
        assertEquals(Datasciencepipelines.ManagementState.MANAGED, cluster.getSpec().getComponents().getDatasciencepipelines().getManagementState());
        assertEquals(Workbenches.ManagementState.MANAGED, cluster.getSpec().getComponents().getWorkbenches().getManagementState());
        assertEquals(Modelmeshserving.ManagementState.MANAGED, cluster.getSpec().getComponents().getModelmeshserving().getManagementState());
        assertEquals(Ray.ManagementState.MANAGED, cluster.getSpec().getComponents().getRay().getManagementState());
        assertEquals(Trustyai.ManagementState.MANAGED, cluster.getSpec().getComponents().getTrustyai().getManagementState());
    }
}
