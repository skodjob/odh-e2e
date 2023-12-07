/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.framework.listeners;

import io.odh.test.framework.manager.ResourceManager;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * jUnit5 specific class which listening on test callbacks
 */
public class ResourceManagerDeleteHandler implements AfterAllCallback, AfterEachCallback {
    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        ResourceManager.getInstance().switchToClassResourceStack();
        ResourceManager.getInstance().deleteResources();
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        ResourceManager.getInstance().switchToTestResourceStack();
        ResourceManager.getInstance().deleteResources();
    }
}
