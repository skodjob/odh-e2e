/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e.continuous;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.odh.test.Environment;
import io.odh.test.OdhConstants;
import io.odh.test.TestSuite;
import io.odh.test.TestUtils;
import io.odh.test.e2e.Abstract;
import io.odh.test.framework.manager.resources.DataScienceClusterResource;
import io.odh.test.install.InstallTypes;
import io.odh.test.utils.CsvUtils;
import io.opendatahub.datasciencecluster.v1.DataScienceCluster;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Codeflare;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Dashboard;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Datasciencepipelines;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Kserve;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Kueue;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Modelmeshserving;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Ray;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Workbenches;
import io.opendatahub.v1alpha.OdhDashboardConfig;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(TestSuite.CONTINUOUS)
public class DataScienceClusterST extends Abstract {

    private static final String DS_CLUSTER_NAME = "default";
    private static final String DS_DASHBOARD_CONFIG_NAME = "odh-dashboard-config";
    MixedOperation<DataScienceCluster, KubernetesResourceList<DataScienceCluster>, Resource<DataScienceCluster>> dataScienceProjectCli;
    MixedOperation<OdhDashboardConfig, KubernetesResourceList<OdhDashboardConfig>, Resource<OdhDashboardConfig>> dashboardConfigCli;

    @BeforeAll
    void init() {
        dataScienceProjectCli = DataScienceClusterResource.dataScienceCLusterClient();
        dashboardConfigCli  = KubeResourceManager.getKubeClient().getClient().resources(OdhDashboardConfig.class);
    }

    @Test
    void checkDataScienceClusterExists() {
        DataScienceCluster cluster = dataScienceProjectCli.inNamespace(OdhConstants.CONTROLLERS_NAMESPACE).withName(DS_CLUSTER_NAME).get();

        assertEquals(Kserve.ManagementState.Managed, cluster.getSpec().getComponents().getKserve().getManagementState());
        assertEquals(Codeflare.ManagementState.Managed, cluster.getSpec().getComponents().getCodeflare().getManagementState());
        assertEquals(Dashboard.ManagementState.Managed, cluster.getSpec().getComponents().getDashboard().getManagementState());
        assertEquals(Ray.ManagementState.Managed, cluster.getSpec().getComponents().getRay().getManagementState());
        assertEquals(Modelmeshserving.ManagementState.Managed, cluster.getSpec().getComponents().getModelmeshserving().getManagementState());
        assertEquals(Datasciencepipelines.ManagementState.Managed, cluster.getSpec().getComponents().getDatasciencepipelines().getManagementState());
        if (!Environment.PRODUCT.equals(Environment.PRODUCT_ODH)
                && Environment.OPERATOR_INSTALL_TYPE.equalsIgnoreCase(InstallTypes.OLM.toString())
                && Objects.requireNonNull(CsvUtils.getOperatorVersionFromCsv()).equals("2.7.0")) {
            // https://issues.redhat.com/browse/RHOAIENG-3234 Remove Kueue from RHOAI 2.7
            assertNull(cluster.getSpec().getComponents().getKueue());
        } else {
            assertEquals(Kueue.ManagementState.Managed, cluster.getSpec().getComponents().getKueue().getManagementState());
        }
        assertEquals(Workbenches.ManagementState.Managed, cluster.getSpec().getComponents().getWorkbenches().getManagementState());
    }

    @Test
    void checkDataScienceClusterStatus() {
        DataScienceCluster cluster = dataScienceProjectCli.inNamespace(OdhConstants.CONTROLLERS_NAMESPACE).withName(DS_CLUSTER_NAME).get();

        assertEquals("Ready", cluster.getStatus().getPhase());
        assertNull(cluster.getStatus().getErrorMessage());

        assertEquals("True", TestUtils.getDscConditionByType(cluster.getStatus().getConditions(), "dashboardReady").getStatus());
        assertEquals("True", TestUtils.getDscConditionByType(cluster.getStatus().getConditions(), "workbenchesReady").getStatus());
        assertEquals("True", TestUtils.getDscConditionByType(cluster.getStatus().getConditions(), "data-science-pipelines-operatorReady").getStatus());
        assertEquals("True", TestUtils.getDscConditionByType(cluster.getStatus().getConditions(), "kserveReady").getStatus());
        assertEquals("True", TestUtils.getDscConditionByType(cluster.getStatus().getConditions(), "codeflareReady").getStatus());
        assertEquals("True", TestUtils.getDscConditionByType(cluster.getStatus().getConditions(), "model-meshReady").getStatus());
        assertEquals("True", TestUtils.getDscConditionByType(cluster.getStatus().getConditions(), "kueueReady").getStatus());
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
