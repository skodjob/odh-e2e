/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e.standard;

import io.odh.test.OdhConstants;
import io.odh.test.TestSuite;
import io.odh.test.framework.manager.ResourceManager;
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
                .withName("default-dsci")
                .endMetadata()
                .withNewSpec()
                .withApplicationsNamespace(OdhConstants.CONTROLLERS_NAMESPACE)
                .withNewMonitoring()
                .withManagementState(Monitoring.ManagementState.MANAGED)
                .withNamespace(OdhConstants.CONTROLLERS_NAMESPACE)
                .endMonitoring()
                .withNewServiceMesh()
                .withManagementState(ServiceMesh.ManagementState.REMOVED)
                .withNewControlPlane()
                .withName("data-science-smcp")
                .withNamespace("istio-system")
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
    }
}
