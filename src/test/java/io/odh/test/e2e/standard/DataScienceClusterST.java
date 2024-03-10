/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e.standard;

import io.odh.test.Environment;
import io.odh.test.TestSuite;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.framework.manager.resources.DataScienceClusterResource;
import io.odh.test.install.InstallTypes;
import io.odh.test.utils.CsvUtils;
import io.odh.test.utils.DscUtils;
import io.opendatahub.datasciencecluster.v1.DataScienceCluster;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Codeflare;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Dashboard;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Datasciencepipelines;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Kserve;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Kueue;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Modelmeshserving;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Ray;
import io.opendatahub.datasciencecluster.v1.datascienceclusterspec.components.Workbenches;
import io.opendatahub.dscinitialization.v1.DSCInitialization;
import io.skodjob.annotations.Contact;
import io.skodjob.annotations.Desc;
import io.skodjob.annotations.Step;
import io.skodjob.annotations.SuiteDoc;
import io.skodjob.annotations.TestDoc;
import io.skodjob.annotations.TestTag;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SuiteDoc(
    description = @Desc("Verifies simple setup of ODH by spin-up operator, setup DSCI, and setup DSC."),
    beforeTestSteps = {
        @Step(value = "Deploy Pipelines Operator", expected = "Pipelines operator is available on the cluster"),
        @Step(value = "Deploy ServiceMesh Operator", expected = "ServiceMesh operator is available on the cluster"),
        @Step(value = "Deploy Serverless Operator", expected = "Serverless operator is available on the cluster"),
        @Step(value = "Install ODH operator", expected = "Operator is up and running and is able to serve it's operands")
    },
    afterTestSteps = {
        @Step(value = "Delete ODH operator and all created resources", expected = "Operator is removed and all other resources as well")
    },
    tags = {
        @TestTag(value = TestSuite.SMOKE)
    }
)
@Tag(TestSuite.SMOKE)
@DisabledIfEnvironmentVariable(
        named = Environment.SKIP_DEPLOY_DSCI_DSC_ENV,
        matches = "true",
        disabledReason = "Default DSCI and DSC deployed no need to run test")
public class DataScienceClusterST extends StandardAbstract {

    private static final String DS_PROJECT_NAME = "test-dsp";

    @TestDoc(
        description = @Desc("Creates default DSCI and DSC and see if operator configure everything properly. Check that operator set status of the resources properly."),
        contact = @Contact(name = "David Kornel", email = "dkornel@redhat.com"),
        steps = {
            @Step(value = "Create default DSCI", expected = "DSCI is created and ready"),
            @Step(value = "Create default DSC", expected = "DSC is created and ready"),
            @Step(value = "Check that DSC has expected states for all components", expected = "DSC status is set properly based on configuration")
        },
        tags = {
            @TestTag(value = TestSuite.SMOKE)
        }
    )
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
        if (!Environment.PRODUCT.equals(Environment.PRODUCT_DEFAULT)
                && Environment.OPERATOR_INSTALL_TYPE.equalsIgnoreCase(InstallTypes.OLM.toString())
                && Objects.requireNonNull(CsvUtils.getOperatorVersionFromCsv()).equals("2.7.0")) {
            // https://issues.redhat.com/browse/RHOAIENG-3234 Remove Kueue from RHOAI 2.7
            assertNull(cluster.getSpec().getComponents().getKueue());
        } else {
            assertEquals(Kueue.ManagementState.MANAGED, cluster.getSpec().getComponents().getKueue().getManagementState());
        }
    }
}
