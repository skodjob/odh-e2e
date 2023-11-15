/*
 * Copyright Tealc authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e.continuous;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.odh.test.TestConstants;
import io.odh.test.e2e.Abstract;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag("continuous")
public class DataScienceClusterIT extends Abstract {

    private final String DS_PROJECT_NAME = "default";
    MixedOperation<DataScienceCluster, KubernetesResourceList<DataScienceCluster>, Resource<DataScienceCluster>> cli;

    @BeforeAll
    void init() {
        cli = kubeClient.dataScienceClusterClient();
    }

    @Test
    void checkDataScienceClusterExists() {
        DataScienceCluster cluster = cli.inNamespace(TestConstants.ODH_NAMESPACE).withName(DS_PROJECT_NAME).get();

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
        DataScienceCluster cluster = cli.inNamespace(TestConstants.ODH_NAMESPACE).withName(DS_PROJECT_NAME).get();

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
}
