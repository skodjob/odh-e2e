/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.utils;

import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.readiness.Readiness;
import io.odh.test.TestConstants;
import io.odh.test.TestUtils;
import io.odh.test.framework.manager.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

public class PodUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(PodUtils.class);
    private static final long DELETION_TIMEOUT = Duration.ofMinutes(5).toMillis();
    private static final long READINESS_TIMEOUT = Duration.ofMinutes(10).toMillis();

    private PodUtils() { }

    public static void waitForPodsReady(String namespaceName, LabelSelector selector, int expectPods, boolean containers, Runnable onTimeout) {
        TestUtils.waitFor("readiness of all Pods matching: " + selector,
                TestConstants.GLOBAL_POLL_INTERVAL_MEDIUM, READINESS_TIMEOUT,
            () -> {
                List<Pod> pods = ResourceManager.getClient().listPods(namespaceName, selector);
                if (pods.isEmpty() && expectPods == 0) {
                    LOGGER.debug("Expected Pods are ready");
                    return true;
                }
                if (pods.isEmpty()) {
                    LOGGER.debug("Pods matching: {}/{} are not ready", namespaceName, selector);
                    return false;
                }
                if (pods.size() != expectPods) {
                    LOGGER.debug("Expected Pods: {}/{} are not ready", namespaceName, selector);
                    return false;
                }
                for (Pod pod : pods) {
                    if (!Readiness.isPodReady(pod)) {
                        LOGGER.debug("Pod not ready: {}/{}", namespaceName, pod.getMetadata().getName());
                        return false;
                    } else {
                        if (containers) {
                            for (ContainerStatus cs : pod.getStatus().getContainerStatuses()) {
                                if (!Boolean.TRUE.equals(cs.getReady())) {
                                    LOGGER.debug("Container: {} of Pod: {}/{} not ready", namespaceName, pod.getMetadata().getName(), cs.getName());
                                    return false;
                                }
                            }
                        }
                    }
                }
                LOGGER.info("Pods matching: {}/{} are ready", namespaceName, selector);
                return true;
            }, onTimeout);
    }
}
