/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e.standard;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.odh.test.e2e.Abstract;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("standard")
public class DataScienceClusterIT extends Abstract {

    private static final String DS_PROJECT_NAME = "test-dsp";
    private static final String DS_PROJECT_NAMESPACE = "test-ns-ds";
    MixedOperation<DataScienceCluster, KubernetesResourceList<DataScienceCluster>, Resource<DataScienceCluster>> cli;

    @BeforeAll
    void init() {
        cli = kubeClient.dataScienceClusterClient();
    }

    @AfterAll
    void clean() {
        cli.inNamespace(DS_PROJECT_NAMESPACE).withName(DS_PROJECT_NAME).delete();
        kubeClient.getClient().namespaces().withName(DS_PROJECT_NAMESPACE).delete();
    }

    @Test
    void createDataScienceCluster() {
        if (!kubeClient.namespaceExists(DS_PROJECT_NAMESPACE)) {
            kubeClient.getClient()
                    .namespaces()
                    .resource(new NamespaceBuilder().withNewMetadata().withName(DS_PROJECT_NAMESPACE).endMetadata().build())
                    .create();
        }

        DataScienceCluster c = new DataScienceClusterBuilder()
                .withNewMetadata()
                .withName(DS_PROJECT_NAME)
                .withNamespace(DS_PROJECT_NAMESPACE)
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

        cli.resource(c).create();

        DataScienceCluster cluster = cli.inNamespace(DS_PROJECT_NAMESPACE).withName(DS_PROJECT_NAME).get();

        assertEquals(Kserve.ManagementState.MANAGED, cluster.getSpec().getComponents().getKserve().getManagementState());
        assertEquals(Codeflare.ManagementState.MANAGED, cluster.getSpec().getComponents().getCodeflare().getManagementState());
        assertEquals(Dashboard.ManagementState.MANAGED, cluster.getSpec().getComponents().getDashboard().getManagementState());
        assertEquals(Datasciencepipelines.ManagementState.MANAGED, cluster.getSpec().getComponents().getDatasciencepipelines().getManagementState());
        assertEquals(Workbenches.ManagementState.MANAGED, cluster.getSpec().getComponents().getWorkbenches().getManagementState());
    }
}
