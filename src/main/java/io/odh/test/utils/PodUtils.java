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
import java.util.Map;
import java.util.stream.Collectors;

public class PodUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(PodUtils.class);
    private static final long DELETION_TIMEOUT = Duration.ofMinutes(5).toMillis();
    private static final long READINESS_TIMEOUT = Duration.ofMinutes(10).toMillis();

    private PodUtils() { }

    public static void waitForPodsReady(String namespaceName, boolean containers, Runnable onTimeout) {
        TestUtils.waitFor("readiness of all Pods matching in namespace " + namespaceName,
                TestConstants.GLOBAL_POLL_INTERVAL_MEDIUM, READINESS_TIMEOUT,
                () -> {
                    List<Pod> pods = ResourceManager.getClient().listPods(namespaceName);
                    if (pods.isEmpty()) {
                        LOGGER.debug("Expected Pods are not ready!");
                        return false;
                    }
                    for (Pod pod : pods) {
                        if (!(Readiness.isPodReady(pod) || Readiness.isPodSucceeded(pod))) {
                            LOGGER.debug("Pod not ready: {}/{}", namespaceName, pod.getMetadata().getName());
                            return false;
                        } else {
                            if (containers) {
                                for (ContainerStatus cs : pod.getStatus().getContainerStatuses()) {
                                    if (!(Boolean.TRUE.equals(cs.getReady())
                                            || cs.getState().getAdditionalProperties().getOrDefault("Reason", "none").equals("Completed"))) {
                                        LOGGER.debug("Container: {} of Pod: {}/{} not ready", namespaceName, pod.getMetadata().getName(), cs.getName());
                                        return false;
                                    }
                                }
                            }
                        }
                    }
                    LOGGER.info("Pods in namespace {} are ready", namespaceName);
                    return true;
                }, onTimeout);
    }

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
                    if (!(Readiness.isPodReady(pod) || Readiness.isPodSucceeded(pod))) {
                        LOGGER.debug("Pod not ready: {}/{}", namespaceName, pod.getMetadata().getName());
                        return false;
                    } else {
                        if (containers) {
                            for (ContainerStatus cs : pod.getStatus().getContainerStatuses()) {
                                if (!(Boolean.TRUE.equals(cs.getReady())
                                        || cs.getState().getAdditionalProperties().getOrDefault("Reason", "none").equals("Completed"))) {
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

    /**
     * Returns a map of resource name to resource version for all the pods in the given {@code namespace}
     * matching the given {@code selector}.
     */
    public static Map<String, String> podSnapshot(String namespaceName, LabelSelector selector) {
        List<Pod> pods = ResourceManager.getClient().listPods(namespaceName, selector);
        return pods.stream()
                .collect(
                        Collectors.toMap(pod -> pod.getMetadata().getName(),
                                pod -> pod.getMetadata().getUid()));
    }

    public static void verifyThatPodsAreStable(String namespaceName, LabelSelector labelSelector) {
        int[] stabilityCounter = {0};
        String phase = "Running";

        TestUtils.waitFor(String.format("Pods in namespace '%s' with LabelSelector %s stability in phase %s", namespaceName, labelSelector, phase), TestConstants.GLOBAL_POLL_INTERVAL_SHORT, TestConstants.GLOBAL_TIMEOUT,
                () -> {
                    List<Pod> existingPod = ResourceManager.getClient().listPods(namespaceName, labelSelector);
                    LOGGER.debug("Working with the following pods: {}", existingPod.stream().map(p -> p.getMetadata().getName()).toList());

                    for (Pod pod : existingPod) {
                        if (pod == null) {
                            continue;
                        }
                        if (pod.getStatus().getPhase().equals(phase)) {
                            LOGGER.debug("Pod: {}/{} is in the {} state. Remaining seconds for Pod to be stable {}",
                                    namespaceName, pod.getMetadata().getName(), pod.getStatus().getPhase(),
                                    TestConstants.GLOBAL_STABILITY_TIME - (TestConstants.GLOBAL_POLL_INTERVAL_SHORT / 1000 * stabilityCounter[0]));
                        } else {
                            LOGGER.warn("Pod: {}/{} is not stable in phase following phase {} ({}) reset the stability counter from {}s to {}s",
                                    namespaceName, pod.getMetadata().getName(), pod.getStatus().getPhase(), phase, stabilityCounter[0], 0);
                            stabilityCounter[0] = 0;
                            return false;
                        }
                    }
                    stabilityCounter[0]++;

                    if (stabilityCounter[0] == TestConstants.GLOBAL_STABILITY_TIME / (TestConstants.GLOBAL_POLL_INTERVAL_SHORT / 1000)) {
                        LOGGER.info("All Pods are stable {}", existingPod.stream().map(p -> p.getMetadata().getName()).collect(Collectors.joining(" ,")));
                        return true;
                    }
                    return false;
                });
    }
}
