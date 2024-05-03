/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.utils;

import io.odh.test.TestConstants;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.wait.Wait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class NamespaceUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(NamespaceUtils.class);
    private static final long DELETION_TIMEOUT = Duration.ofMinutes(2).toMillis();

    private NamespaceUtils() { }

    public static void waitForNamespaceReadiness(String name) {
        LOGGER.info("Waiting for Namespace: {} readiness", name);

        Wait.until("Namespace: " + name, TestConstants.GLOBAL_POLL_INTERVAL_SHORT, DELETION_TIMEOUT,
                () -> KubeResourceManager.getKubeClient().getClient().namespaces().withName(name).get() != null);
        LOGGER.info("Namespace: {} is ready", name);
    }

    public static void waitForNamespaceDeletion(String name) {
        LOGGER.info("Waiting for Namespace: {} deletion", name);

        Wait.until("Namespace: " + name, TestConstants.GLOBAL_POLL_INTERVAL_SHORT, DELETION_TIMEOUT,
            () -> KubeResourceManager.getKubeClient().getClient().namespaces().withName(name).get() == null);
        LOGGER.info("Namespace: {} was deleted", name);
    }
}
