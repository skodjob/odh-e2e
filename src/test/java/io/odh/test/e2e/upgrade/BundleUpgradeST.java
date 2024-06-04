/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e.upgrade;

import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.odh.test.Environment;
import io.odh.test.OdhConstants;
import io.odh.test.TestSuite;
import io.odh.test.install.BundleInstall;
import io.odh.test.utils.DeploymentUtils;
import io.odh.test.utils.UpgradeUtils;
import io.skodjob.annotations.Contact;
import io.skodjob.annotations.Desc;
import io.skodjob.annotations.Step;
import io.skodjob.annotations.SuiteDoc;
import io.skodjob.annotations.TestDoc;
import io.skodjob.annotations.TestTag;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.utils.PodUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

@SuiteDoc(
    description = @Desc("Verifies upgrade path from previously released version to latest available build. Operator installation and upgrade is done via bundle of yaml files."),
    beforeTestSteps = {
        @Step(value = "Deploy Pipelines Operator", expected = "Pipelines operator is available on the cluster"),
        @Step(value = "Deploy ServiceMesh Operator", expected = "ServiceMesh operator is available on the cluster"),
        @Step(value = "Deploy Serverless Operator", expected = "Serverless operator is available on the cluster")
    },
    afterTestSteps = {
        @Step(value = "Delete all ODH related resources in the cluster", expected = "All ODH related resources are gone")
    },
    tags = {
        @TestTag(value = TestSuite.BUNDLE_UPGRADE)
    }
)
@Tag(TestSuite.BUNDLE_UPGRADE)
public class BundleUpgradeST extends UpgradeAbstract {

    private static final Logger LOGGER = LoggerFactory.getLogger(BundleUpgradeST.class);

    BundleInstall baseBundle;
    BundleInstall upgradeBundle;

    @AfterAll
    void clean() {
        if (upgradeBundle != null) {
            upgradeBundle.deleteWithoutResourceManager();
        }
    }

    @TestDoc(
        description = @Desc("Creates default DSCI and DSC and see if operator configure everything properly. Check that operator set status of the resources properly."),
        contact = @Contact(name = "David Kornel", email = "dkornel@redhat.com"),
        steps = {
            @Step(value = "Install operator via bundle of yaml files with specific version", expected = "Operator is up and running"),
            @Step(value = "Deploy DSC (see UpgradeAbstract for more info)", expected = "DSC is created and ready"),
            @Step(value = "Deploy Notebook to namespace test-odh-notebook-upgrade", expected = "All related pods are up and running. Notebook is in ready state."),
            @Step(value = "Apply latest yaml files with latest Operator version", expected = "Yaml file is applied"),
            @Step(value = "Wait for RollingUpdate of Operator pod to a new version", expected = "Operator update is finished and pod is up and running"),
            @Step(value = "Verify that Dashboard pods are stable for 2 minutes", expected = "Dashboard pods are stable por 2 minutes after upgrade"),
            @Step(value = "Verify that Notebook pods are stable for 2 minutes", expected = "Notebook pods are stable por 2 minutes after upgrade"),
            @Step(value = "Check that ODH operator doesn't contain any error logs", expected = "ODH operator log is error free")
        },
        tags = {
            @TestTag(value = TestSuite.BUNDLE_UPGRADE)
        }
    )
    @Test
    void testUpgradeBundle() throws IOException {
        LOGGER.info("Install base version");
        baseBundle = new BundleInstall(Environment.INSTALL_FILE_PREVIOUS_PATH);
        baseBundle.disableModifyOperatorImage();
        baseBundle.createWithoutResourceManager();

        String dsProjectName = "test-notebooks-upgrade";
        String ntbName = "test-odh-notebook";
        String ntbNamespace = "test-odh-notebook-upgrade";

        deployDsc(dsProjectName);
        deployNotebook(ntbNamespace, ntbName);

        Map<String, String> operatorSnapshot = DeploymentUtils.depSnapshot(baseBundle.getNamespace(), baseBundle.getDeploymentName());

        LabelSelector lblSelector = new LabelSelectorBuilder()
                .withMatchLabels(Map.of("app", ntbName))
                .build();

        PodUtils.waitForPodsReady(ntbNamespace, lblSelector, 1, true, () -> { });

        LOGGER.info("Upgrade to latest version");
        upgradeBundle = new BundleInstall(Environment.INSTALL_FILE_PATH);
        upgradeBundle.createWithoutResourceManager();

        DeploymentUtils.waitTillDepHasRolled(baseBundle.getNamespace(), baseBundle.getDeploymentName(), operatorSnapshot);

        LabelSelector labelSelector = KubeResourceManager.getKubeClient().getClient().apps().deployments()
                .inNamespace(OdhConstants.CONTROLLERS_NAMESPACE).withName(OdhConstants.DASHBOARD_CONTROLLER).get().getSpec().getSelector();
        PodUtils.verifyThatPodsAreStable(OdhConstants.CONTROLLERS_NAMESPACE, labelSelector);
        Date operatorLogCheckTimestamp = new Date();

        // Verify that NTB pods are stable
        PodUtils.waitForPodsReady(ntbNamespace, lblSelector, 1, true, () -> { });
        // Check logs in operator pod
        UpgradeUtils.deploymentLogIsErrorEmpty(baseBundle.getNamespace(), baseBundle.getDeploymentName(), operatorLogCheckTimestamp);
    }
}
