/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e.upgrade;

import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.InstallPlan;
import io.odh.test.Environment;
import io.odh.test.OdhConstants;
import io.odh.test.TestSuite;
import io.odh.test.TestUtils;
import io.odh.test.install.OlmInstall;
import io.odh.test.utils.DeploymentUtils;
import io.odh.test.utils.UpgradeUtils;
import io.skodjob.annotations.Contact;
import io.skodjob.annotations.Desc;
import io.skodjob.annotations.Step;
import io.skodjob.annotations.SuiteDoc;
import io.skodjob.annotations.TestDoc;
import io.skodjob.annotations.Label;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.utils.KubeUtils;
import io.skodjob.testframe.utils.PodUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@SuiteDoc(
    description = @Desc("Verifies upgrade path from previously released version to latest available build. Operator installation and upgrade is done via OLM."),
    beforeTestSteps = {
        @Step(value = "Deploy Pipelines Operator", expected = "Pipelines operator is available on the cluster"),
        @Step(value = "Deploy ServiceMesh Operator", expected = "ServiceMesh operator is available on the cluster"),
        @Step(value = "Deploy Serverless Operator", expected = "Serverless operator is available on the cluster")
    },
    labels = {
        @Label(value = TestSuite.OLM_UPGRADE)
    }
)
@Tag(TestSuite.OLM_UPGRADE)
public class OlmUpgradeST extends UpgradeAbstract {

    private static final Logger LOGGER = LoggerFactory.getLogger(OlmUpgradeST.class);
    private static final String DS_PROJECT_NAME = "upgrade-dsc";

    private final String startingVersion = Environment.OLM_UPGRADE_STARTING_VERSION;

    @TestDoc(
        description = @Desc("Creates default DSCI and DSC and see if operator configure everything properly. Check that operator set status of the resources properly."),
        contact = @Contact(name = "Jakub Stejskal", email = "jstejska@redhat.com"),
        steps = {
            @Step(value = "Install operator via OLM with manual approval and specific version", expected = "Operator is up and running"),
            @Step(value = "Deploy DSC (see UpgradeAbstract for more info)", expected = "DSC is created and ready"),
            @Step(value = "Deploy Notebook to namespace test-odh-notebook-upgrade", expected = "All related pods are up and running. Notebook is in ready state."),
            @Step(value = "Approve install plan for new version", expected = "Install plan is approved"),
            @Step(value = "Wait for RollingUpdate of Operator pod to a new version", expected = "Operator update is finished and pod is up and running"),
            @Step(value = "Verify that Dashboard pods are stable for 2 minutes", expected = "Dashboard pods are stable por 2 minutes after upgrade"),
            @Step(value = "Verify that Notebook pods are stable for 2 minutes", expected = "Notebook pods are stable por 2 minutes after upgrade"),
            @Step(value = "Check that ODH operator doesn't contain any error logs", expected = "ODH operator log is error free")
        },
        labels = {
            @Label(value = TestSuite.OLM_UPGRADE)
        }
    )
    @Test
    void testUpgradeOlm() throws IOException, InterruptedException {
        String ntbName = "test-odh-notebook";
        String ntbNamespace = "test-odh-notebook-upgrade";

        OlmInstall olmInstall = new OlmInstall();
        olmInstall.setApproval("Manual");
        olmInstall.setStartingCsv(olmInstall.getOperatorName() + "." + startingVersion);
        olmInstall.createManual();

        // Approve install plan created for older version
        TestUtils.waitForInstallPlan(olmInstall.getNamespace(), olmInstall.getOperatorName() + "." + startingVersion);
        InstallPlan ip = KubeUtils.getNonApprovedInstallPlan(olmInstall.getNamespace(), olmInstall.getOperatorName() + "." + startingVersion);
        KubeUtils.approveInstallPlan(olmInstall.getNamespace(), ip.getMetadata().getName());
        // Wait for old version readiness
        DeploymentUtils.waitForDeploymentReady(olmInstall.getNamespace(), olmInstall.getDeploymentName());

        // Make snapshot of current operator
        Map<String, String> operatorSnapshot = DeploymentUtils.depSnapshot(olmInstall.getNamespace(), olmInstall.getDeploymentName());

        // Deploy DSC
        deployDsc(DS_PROJECT_NAME);
        deployNotebook(ntbNamespace, ntbName);

        LabelSelector lblSelector = new LabelSelectorBuilder()
                .withMatchLabels(Map.of("app", ntbName))
                .build();

        PodUtils.waitForPodsReady(ntbNamespace, lblSelector, 1, true, () -> { });

        LOGGER.info("Upgrade to next available version in OLM catalog");
        // Approve upgrade to newer version
        TestUtils.waitForInstallPlan(olmInstall.getNamespace(), olmInstall.getCsvName());
        ip = KubeUtils.getNonApprovedInstallPlan(olmInstall.getNamespace(), olmInstall.getCsvName());
        KubeUtils.approveInstallPlan(olmInstall.getNamespace(), ip.getMetadata().getName());
        // Wait for operator RU
        DeploymentUtils.waitTillDepHasRolled(olmInstall.getNamespace(), olmInstall.getDeploymentName(), operatorSnapshot);

        // Wait for pod stability for Dashboard
        LabelSelector labelSelector = KubeResourceManager.getKubeClient().getClient()
                .apps().deployments().inNamespace(OdhConstants.CONTROLLERS_NAMESPACE).withName(OdhConstants.DASHBOARD_CONTROLLER).get().getSpec().getSelector();
        PodUtils.verifyThatPodsAreStable(OdhConstants.CONTROLLERS_NAMESPACE, labelSelector);
        Instant operatorLogCheckTimestamp = Instant.now();

        // Verify that NTB pods are stable
        PodUtils.waitForPodsReady(ntbNamespace, lblSelector, 1, true, () -> { });
        // Check logs in operator pod
        UpgradeUtils.deploymentLogIsErrorEmpty(olmInstall.getNamespace(), olmInstall.getDeploymentName(), operatorLogCheckTimestamp);
    }
}
