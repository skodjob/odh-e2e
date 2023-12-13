/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.framework.listeners;

import io.odh.test.framework.logs.LogCollector;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * jUnit5 specific class which listening on test exception callbacks
 */
public class TestExceptionCallbackListener implements TestExecutionExceptionHandler, LifecycleMethodExecutionExceptionHandler {
    static final Logger LOGGER = LoggerFactory.getLogger(TestExceptionCallbackListener.class);

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        LOGGER.error("Test failed at {} : {}", "Test execution", throwable.getMessage(), throwable);
        LogCollector.saveKubernetesState(context, throwable);
    }

    @Override
    public void handleBeforeAllMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        LOGGER.error("Test failed at {} : {}", "Test before all", throwable.getMessage(), throwable);
        LogCollector.saveKubernetesState(context, throwable);
    }

    @Override
    public void handleBeforeEachMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        LOGGER.error("Test failed at {} : {}", "Test before each", throwable.getMessage(), throwable);
        LogCollector.saveKubernetesState(context, throwable);
    }

    @Override
    public void handleAfterEachMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        LOGGER.error("Test failed at {} : {}", "Test after each", throwable.getMessage(), throwable);
        LogCollector.saveKubernetesState(context, throwable);
    }

    @Override
    public void handleAfterAllMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        LOGGER.error("Test failed at {} : {}", "Test after all", throwable.getMessage(), throwable);
        LogCollector.saveKubernetesState(context, throwable);
    }
}
