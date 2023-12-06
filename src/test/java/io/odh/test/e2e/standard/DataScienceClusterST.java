/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e.standard;

import io.odh.test.framework.manager.ResourceManager;
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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("smoke")
public class DataScienceClusterST extends StandardAbstract {

    private static final String DS_PROJECT_NAME = "test-dsp";

    @Test
    void createDataScienceCluster() {
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

        ResourceManager.getInstance().createResourceWithWait(c);

        DataScienceCluster cluster = ResourceManager.getClient().dataScienceClusterClient().withName(DS_PROJECT_NAME).get();

        assertEquals(Kserve.ManagementState.MANAGED, cluster.getSpec().getComponents().getKserve().getManagementState());
        assertEquals(Codeflare.ManagementState.MANAGED, cluster.getSpec().getComponents().getCodeflare().getManagementState());
        assertEquals(Dashboard.ManagementState.MANAGED, cluster.getSpec().getComponents().getDashboard().getManagementState());
        assertEquals(Datasciencepipelines.ManagementState.MANAGED, cluster.getSpec().getComponents().getDatasciencepipelines().getManagementState());
        assertEquals(Workbenches.ManagementState.MANAGED, cluster.getSpec().getComponents().getWorkbenches().getManagementState());
    }
}
