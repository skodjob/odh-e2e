/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e.standard;

import io.odh.test.Environment;
import io.odh.test.TestSuite;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.framework.manager.resources.DataScienceClusterResource;
import io.odh.test.utils.DscUtils;
import io.opendatahub.datasciencecluster.v1.DataScienceCluster;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Codeflare;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Dashboard;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Datasciencepipelines;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Kserve;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Modelmeshserving;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Ray;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Trustyai;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Workbenches;
import io.opendatahub.dscinitialization.v1.DSCInitialization;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(TestSuite.SMOKE)
@DisabledIfEnvironmentVariable(
        named = Environment.SKIP_DEPLOY_DSCI_DSC_ENV,
        matches = "true",
        disabledReason = "Default DSCI and DSC deployed no need to run test")
public class DataScienceClusterST extends StandardAbstract {

    private static final String DS_PROJECT_NAME = "test-dsp";

    @Test
    void createDataScienceCluster() {

        // Create DSCI
        DSCInitialization dsci = DscUtils.getBasicDSCI();
        // Create DSC
        DataScienceCluster c = DscUtils.getBasicDSC(DS_PROJECT_NAME);

        ResourceManager.getInstance().createResourceWithWait(dsci);
        ResourceManager.getInstance().createResourceWithWait(c);

        DataScienceCluster cluster = DataScienceClusterResource.dataScienceCLusterClient().withName(DS_PROJECT_NAME).get();

        assertEquals(Kserve.ManagementState.MANAGED, cluster.getSpec().getComponents().getKserve().getManagementState());
        assertEquals(Codeflare.ManagementState.MANAGED, cluster.getSpec().getComponents().getCodeflare().getManagementState());
        assertEquals(Dashboard.ManagementState.MANAGED, cluster.getSpec().getComponents().getDashboard().getManagementState());
        assertEquals(Datasciencepipelines.ManagementState.MANAGED, cluster.getSpec().getComponents().getDatasciencepipelines().getManagementState());
        assertEquals(Workbenches.ManagementState.MANAGED, cluster.getSpec().getComponents().getWorkbenches().getManagementState());
        assertEquals(Modelmeshserving.ManagementState.MANAGED, cluster.getSpec().getComponents().getModelmeshserving().getManagementState());
        assertEquals(Ray.ManagementState.MANAGED, cluster.getSpec().getComponents().getRay().getManagementState());
        assertEquals(Trustyai.ManagementState.MANAGED, cluster.getSpec().getComponents().getTrustyai().getManagementState());
    }
}
