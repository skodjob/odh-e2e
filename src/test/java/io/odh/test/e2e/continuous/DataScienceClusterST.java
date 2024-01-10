/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e.continuous;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.odh.test.OdhConstants;
import io.odh.test.TestSuite;
import io.odh.test.e2e.Abstract;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.framework.manager.resources.DataScienceClusterResource;
import io.odh.test.platform.KubeUtils;
import io.opendatahub.datasciencecluster.v1.DataScienceCluster;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Codeflare;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Dashboard;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Datasciencepipelines;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Kserve;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Modelmeshserving;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Ray;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Trustyai;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Workbenches;
import io.opendatahub.v1alpha.OdhDashboardConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(TestSuite.CONTINUOUS)
public class DataScienceClusterST extends Abstract {

    // TODO on my cluster this is default-dsc, cluster created with existing jenkins automation in PSI
    private static final String DS_CLUSTER_NAME = "default-dsc";
    private static final String DS_DASHBOARD_CONFIG_NAME = "odh-dashboard-config";
    MixedOperation<DataScienceCluster, KubernetesResourceList<DataScienceCluster>, Resource<DataScienceCluster>> dataScienceProjectCli;
    MixedOperation<OdhDashboardConfig, KubernetesResourceList<OdhDashboardConfig>, Resource<OdhDashboardConfig>> dashboardConfigCli;

    @BeforeAll
    void init() {
        dataScienceProjectCli = DataScienceClusterResource.dataScienceCLusterClient();
        dashboardConfigCli  = ResourceManager.getClient().dashboardConfigClient();
    }

    @Test
    void checkDataScienceClusterExists() {
        DataScienceCluster cluster = dataScienceProjectCli.inNamespace(OdhConstants.CONTROLLERS_NAMESPACE).withName(DS_CLUSTER_NAME).get();

        assertEquals(Kserve.ManagementState.MANAGED, cluster.getSpec().getComponents().getKserve().getManagementState());
        assertEquals(Codeflare.ManagementState.MANAGED, cluster.getSpec().getComponents().getCodeflare().getManagementState());
        assertEquals(Dashboard.ManagementState.MANAGED, cluster.getSpec().getComponents().getDashboard().getManagementState());
        assertEquals(Ray.ManagementState.MANAGED, cluster.getSpec().getComponents().getRay().getManagementState());
        assertEquals(Modelmeshserving.ManagementState.MANAGED, cluster.getSpec().getComponents().getModelmeshserving().getManagementState());
        assertEquals(Datasciencepipelines.ManagementState.MANAGED, cluster.getSpec().getComponents().getDatasciencepipelines().getManagementState());
        assertEquals(Trustyai.ManagementState.MANAGED, cluster.getSpec().getComponents().getTrustyai().getManagementState());
        assertEquals(Workbenches.ManagementState.MANAGED, cluster.getSpec().getComponents().getWorkbenches().getManagementState());
    }

    @Test
    void checkDataScienceClusterStatus() {
        DataScienceCluster cluster = dataScienceProjectCli.inNamespace(OdhConstants.CONTROLLERS_NAMESPACE).withName(DS_CLUSTER_NAME).get();

        assertEquals("Ready", cluster.getStatus().getPhase());
        assertNull(cluster.getStatus().getErrorMessage());

        assertEquals("True", KubeUtils.getDscConditionByType(cluster.getStatus().getConditions(), "dashboardReady").getStatus());
        //TODO investigate why it is always switch state
        //assertEquals("True", KubeUtils.getDscConditionByType(cluster.getStatus().getConditions(), "workbenchesReady").getStatus());
        //assertEquals("True", KubeUtils.getDscConditionByType(cluster.getStatus().getConditions(), "data-science-pipelines-operatorReady").getStatus());
        //assertEquals("True", KubeUtils.getDscConditionByType(cluster.getStatus().getConditions(), "kserveReady").getStatus());
        //assertEquals("True", KubeUtils.getDscConditionByType(cluster.getStatus().getConditions(), "codeflareReady").getStatus());
        //assertEquals("True", KubeUtils.getDscConditionByType(cluster.getStatus().getConditions(), "model-meshReady").getStatus());
        //assertEquals("True", KubeUtils.getDscConditionByType(cluster.getStatus().getConditions(), "trustyaiReady").getStatus());
    }

    @Test
    void checkDataScienceDashboard() {
        OdhDashboardConfig dashboard = dashboardConfigCli.inNamespace(OdhConstants.CONTROLLERS_NAMESPACE).withName(DS_DASHBOARD_CONFIG_NAME).get();

        assertTrue(dashboard.getSpec().getNotebookController().getEnabled());

        assertFalse(dashboard.getSpec().getDashboardConfig().getDisableInfo());
        assertFalse(dashboard.getSpec().getDashboardConfig().getDisableBiasMetrics());
        assertFalse(dashboard.getSpec().getDashboardConfig().getDisableClusterManager());
        assertFalse(dashboard.getSpec().getDashboardConfig().getDisableCustomServingRuntimes());
        assertFalse(dashboard.getSpec().getDashboardConfig().getDisableKServe());
        assertFalse(dashboard.getSpec().getDashboardConfig().getDisablePipelines());
        assertFalse(dashboard.getSpec().getDashboardConfig().getDisableProjects());
    }
}
