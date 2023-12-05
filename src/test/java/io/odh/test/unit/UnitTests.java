/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.unit;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.odh.test.framework.ExtensionContextParameterResolver;
import io.opendatahub.datasciencecluster.v1.DataScienceCluster;
import io.opendatahub.datasciencecluster.v1.DataScienceClusterBuilder;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.ComponentsBuilder;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Codeflare;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.CodeflareBuilder;
import io.opendatahub.v1alpha.OdhDashboardConfig;
import io.opendatahub.v1alpha.OdhDashboardConfigBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag("unit")
@ExtendWith(ExtensionContextParameterResolver.class)
@EnableKubernetesMockClient(crud = true)
public class UnitTests {

    static final Logger LOGGER = LoggerFactory.getLogger(UnitTests.class);

    static final String SEPARATOR_CHAR = "#";

    private KubernetesClient kubernetesClient;

    private KubernetesMockServer server;

    @BeforeEach
    void before(ExtensionContext extensionContext) {
        LOGGER.info(String.join("", Collections.nCopies(76, SEPARATOR_CHAR)));
        LOGGER.info(String.format("%s.%s-STARTED", extensionContext.getRequiredTestClass().getName(),
                extensionContext.getDisplayName().replace("()", "")));
    }

    @AfterEach
    void after(ExtensionContext extensionContext) {
        LOGGER.info(String.format("%s.%s-FINISHED", extensionContext.getRequiredTestClass().getName(),
                extensionContext.getDisplayName().replace("()", "")));
        LOGGER.info(String.join("", Collections.nCopies(76, SEPARATOR_CHAR)));
    }

    @Test
    void testCreateDeleteDataScienceCluster() {
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

        dsClient.withName(cluster.getMetadata().getName()).delete();

        assertNull(dsClient.withName(cluster.getMetadata().getName()).get());
    }

    @Test
    void testDashBoardConfig() {
        MixedOperation<OdhDashboardConfig, KubernetesResourceList<OdhDashboardConfig>, Resource<OdhDashboardConfig>> dashboardCli =
                kubernetesClient.resources(OdhDashboardConfig.class);

        OdhDashboardConfig dashboard = new OdhDashboardConfigBuilder()
                .withNewMetadata()
                .withName("test-config")
                .endMetadata()
                .withNewSpec()
                .withNewDashboardConfig()
                .withDisableClusterManager(false)
                .withDisableCustomServingRuntimes(false)
                .withDisableKServe(false)
                .endDashboardConfig()
                .endSpec()
                .build();

        dashboardCli.resource(dashboard).create();

        OdhDashboardConfig returned = dashboardCli.withName(dashboard.getMetadata().getName()).get();
        assertFalse(returned.getSpec().getDashboardConfig().getDisableKServe());
    }
}
