/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.framework.listeners;

import io.odh.test.framework.ExtensionContextParameterResolver;
import io.odh.test.LoggerUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(ExtensionContextParameterResolver.class)
public interface TestVisualSeparator {
    Logger LOGGER = LoggerFactory.getLogger(TestVisualSeparator.class);

    @BeforeEach
    default void beforeEachTest(ExtensionContext testContext) {
        LoggerUtils.logSeparator();
        LOGGER.info(String.format("%s.%s-STARTED", testContext.getRequiredTestClass().getName(),
                testContext.getDisplayName().replace("()", "")));
    }

    @AfterEach
    default void afterEachTest(ExtensionContext testContext) {
        LOGGER.info(String.format("%s.%s-FINISHED", testContext.getRequiredTestClass().getName(),
                testContext.getDisplayName().replace("()", "")));
        LoggerUtils.logSeparator();
    }
}
