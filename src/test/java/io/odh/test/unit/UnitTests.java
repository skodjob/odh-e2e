package io.odh.test.unit;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.opendatahub.datasciencecluster.v1.DataScienceCluster;
import io.opendatahub.datasciencecluster.v1.DataScienceClusterBuilder;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.ComponentsBuilder;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Codeflare;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.CodeflareBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnableKubernetesMockClient(crud = true)
public class UnitTests {
    private KubernetesClient kubernetesClient;
    private KubernetesMockServer server;

    @Test
    void testCreateDataScienceCluster() {
        MixedOperation<DataScienceCluster, KubernetesResourceList<DataScienceCluster>, Resource<DataScienceCluster>> dsClient =
                kubernetesClient.resources(DataScienceCluster.class);

        DataScienceCluster cluster = new DataScienceClusterBuilder()
                .withNewMetadata()
                .withName("Test")
                .endMetadata()
                .withNewSpec()
                .withComponents(
                        new ComponentsBuilder()
                                .withCodeflare(
                                        new CodeflareBuilder()
                                                .withManagementState(Codeflare.ManagementState.MANAGED)
                                                .build())
                                .build())
                .endSpec()
                .build();

        dsClient.resource(cluster).create();

        DataScienceCluster returned = dsClient.withName(cluster.getMetadata().getName()).get();
        assertEquals(Codeflare.ManagementState.MANAGED, returned.getSpec().getComponents().getCodeflare().getManagementState());
    }
}
