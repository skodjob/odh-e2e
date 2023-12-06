/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.utils;

import io.odh.test.TestConstants;
import io.odh.test.TestUtils;
import io.odh.test.framework.manager.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class NamespaceUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(NamespaceUtils.class);
    private static final long DELETION_TIMEOUT = Duration.ofMinutes(2).toMillis();

    private NamespaceUtils() { }

    public static void waitForNamespaceReadiness(String name) {
        LOGGER.info("Waiting for Namespace: {} readiness", name);

        TestUtils.waitFor("Namespace: " + name, TestConstants.GLOBAL_POLL_INTERVAL_SHORT, DELETION_TIMEOUT,
                () -> ResourceManager.getClient().getNamespace(name) != null);
        LOGGER.info("Namespace: {} is ready", name);
    }

    public static void waitForNamespaceDeletion(String name) {
        LOGGER.info("Waiting for Namespace: {} deletion", name);

        TestUtils.waitFor("Namespace: " + name, TestConstants.GLOBAL_POLL_INTERVAL_SHORT, DELETION_TIMEOUT,
            () -> ResourceManager.getClient().getNamespace(name) == null);
        LOGGER.info("Namespace: {} was deleted", name);
    }
}
