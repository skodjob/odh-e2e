/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e.upgrade;

import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.odh.test.Environment;
import io.odh.test.OdhConstants;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.install.BundleInstall;
import io.odh.test.utils.DeploymentUtils;
import io.odh.test.utils.PodUtils;
import io.odh.test.utils.UpgradeUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

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

        LabelSelector labelSelector = ResourceManager.getClient().getDeployment(OdhConstants.CONTROLLERS_NAMESPACE, OdhConstants.DASHBOARD_CONTROLLER).getSpec().getSelector();
        PodUtils.verifyThatPodsAreStable(OdhConstants.CONTROLLERS_NAMESPACE, labelSelector);

        // Verify that NTB pods are stable
        PodUtils.waitForPodsReady(ntbNamespace, lblSelector, 1, true, () -> { });
        // Check logs in operator pod
        UpgradeUtils.deploymentLogIsErrorEmpty(baseBundle.getNamespace(), baseBundle.getDeploymentName());
    }
}
