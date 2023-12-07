/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.framework.listeners;

import io.odh.test.platform.KubeUtils;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class OdhResourceCleaner implements AfterAllCallback {

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        KubeUtils.clearOdhCRDs();
    }
}
