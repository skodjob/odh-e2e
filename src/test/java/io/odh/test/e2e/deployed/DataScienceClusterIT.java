/*
 * Copyright Tealc authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e.deployed;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.odh.test.TestConstants;
import io.odh.test.e2e.Abstract;
import io.opendatahub.datasciencecluster.v1.DataScienceCluster;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Codeflare;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Dashboard;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Datasciencepipelines;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Kserve;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Modelmeshserving;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Ray;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Trustyai;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Workbenches;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataScienceClusterIT extends Abstract {

    @Test
    void checkDataScienceClusterExists() {
        MixedOperation<DataScienceCluster, KubernetesResourceList<DataScienceCluster>, Resource<DataScienceCluster>> cli =
                kubeClient.dataScienceClusterClient();

        DataScienceCluster cluster = cli.inNamespace(TestConstants.ODH_NAMESPACE).withName("default").get();

        assertEquals(cluster.getSpec().getComponents().getKserve().getManagementState(), Kserve.ManagementState.MANAGED);
        assertEquals(cluster.getSpec().getComponents().getCodeflare().getManagementState(), Codeflare.ManagementState.MANAGED);
        assertEquals(cluster.getSpec().getComponents().getDashboard().getManagementState(), Dashboard.ManagementState.MANAGED);
        assertEquals(cluster.getSpec().getComponents().getRay().getManagementState(), Ray.ManagementState.MANAGED);
        assertEquals(cluster.getSpec().getComponents().getModelmeshserving().getManagementState(), Modelmeshserving.ManagementState.MANAGED);
        assertEquals(cluster.getSpec().getComponents().getDatasciencepipelines().getManagementState(), Datasciencepipelines.ManagementState.MANAGED);
        assertEquals(cluster.getSpec().getComponents().getTrustyai().getManagementState(), Trustyai.ManagementState.MANAGED);
        assertEquals(cluster.getSpec().getComponents().getWorkbenches().getManagementState(), Workbenches.ManagementState.MANAGED);
    }
}
