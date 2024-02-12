/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.framework.listeners;

import io.odh.test.Environment;
import io.odh.test.platform.KubeUtils;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class OdhResourceCleaner implements AfterAllCallback {

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        if (!Environment.SKIP_INSTALL_OPERATOR || !Environment.SKIP_DEPLOY_DSCI_DSC) {
            KubeUtils.clearOdhRemainingResources();
        }
    }
}
