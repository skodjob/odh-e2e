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
import io.odh.test.TestSuite;
import io.odh.test.framework.ExtensionContextParameterResolver;
import io.opendatahub.datasciencecluster.v1.DataScienceCluster;
import io.opendatahub.datasciencecluster.v1.DataScienceClusterBuilder;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Codeflare;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Dashboard;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Datasciencepipelines;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Kserve;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Workbenches;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.datasciencepipelines.devflags.ManifestsBuilder;
import io.opendatahub.dscinitialization.v1.DSCInitialization;
import io.opendatahub.dscinitialization.v1.DSCInitializationBuilder;
import io.opendatahub.dscinitialization.v1.dscinitializationspec.Monitoring;
import io.opendatahub.dscinitialization.v1.dscinitializationspec.servicemesh.ControlPlane;
import io.opendatahub.v1alpha.OdhDashboardConfig;
import io.opendatahub.v1alpha.OdhDashboardConfigBuilder;
import io.skodjob.testframe.annotations.TestVisualSeparator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag(TestSuite.UNIT)
@ExtendWith(ExtensionContextParameterResolver.class)
@TestVisualSeparator
@EnableKubernetesMockClient(crud = true)
public class UnitTests {

    private KubernetesClient kubernetesClient;

    private KubernetesMockServer server;

    @Test
    void testCreateDSCInitialization() {
        MixedOperation<DSCInitialization, KubernetesResourceList<DSCInitialization>, Resource<DSCInitialization>> dsciClient =
                kubernetesClient.resources(DSCInitialization.class);

        DSCInitialization dsci = new DSCInitializationBuilder()
                .withNewMetadata()
                .withName("default")
                .endMetadata()
                .withNewSpec()
                .withApplicationsNamespace("opendatahub")
                .withNewMonitoring()
                .withNamespace("monitoring")
                .withManagementState(Monitoring.ManagementState.MANAGED)
                .endMonitoring()
                .withNewServiceMesh()
                .withNewControlPlane()
                .withNamespace("istio")
                .withName("istio")
                .withMetricsCollection(ControlPlane.MetricsCollection.ISTIO)
                .endControlPlane()
                .endServiceMesh()
                .endSpec()
                .build();
        dsciClient.resource(dsci).create();

        DSCInitialization returned = dsciClient.withName(dsci.getMetadata().getName()).get();

        assertEquals("opendatahub", returned.getSpec().getApplicationsNamespace());

        dsciClient.withName(dsci.getMetadata().getName()).delete();

        assertNull(dsciClient.withName(dsci.getMetadata().getName()).get());
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
                .withNewComponents()
                .withNewCodeflare()
                .withManagementState(Codeflare.ManagementState.MANAGED)
                .endCodeflare()
                .withNewDashboard()
                .withManagementState(Dashboard.ManagementState.MANAGED)
                .endDashboard()
                .withNewKserve()
                .withManagementState(Kserve.ManagementState.REMOVED)
                .endKserve()
                .withNewWorkbenches()
                .withManagementState(Workbenches.ManagementState.MANAGED)
                .endWorkbenches()
                .withNewDatasciencepipelines()
                .withManagementState(Datasciencepipelines.ManagementState.MANAGED)
                .withNewDevFlags()
                .withManifests(new ManifestsBuilder().withUri("https://kornys.com").build())
                .endDatasciencepipelinesDevFlags()
                .endDatasciencepipelines()
                .endDatascienceclusterspecComponents()
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
