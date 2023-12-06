/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.framework;

import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.platform.KubeUtils;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

/**
 * jUnit5 specific class which listening on test callbacks
 */
public class TestCallbackListener implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback, AfterEachCallback {

    static final Logger LOGGER = LoggerFactory.getLogger(TestCallbackListener.class);

    static final String SEPARATOR_CHAR = "#";

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        ResourceManager.getInstance().switchToClassResourceStack();
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        LOGGER.info(String.join("", Collections.nCopies(76, SEPARATOR_CHAR)));
        LOGGER.info(String.format("%s.%s-STARTED", extensionContext.getRequiredTestClass().getName(),
                extensionContext.getDisplayName().replace("()", "")));
        ResourceManager.getInstance().switchToTestResourceStack();
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        ResourceManager.getInstance().switchToClassResourceStack();
        ResourceManager.getInstance().deleteResources();
        KubeUtils.clearOdhCRDs();
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        LOGGER.info(String.format("%s.%s-FINISHED", extensionContext.getRequiredTestClass().getName(),
                extensionContext.getDisplayName().replace("()", "")));
        LOGGER.info(String.join("", Collections.nCopies(76, SEPARATOR_CHAR)));
        ResourceManager.getInstance().switchToTestResourceStack();
        ResourceManager.getInstance().deleteResources();
    }
}
