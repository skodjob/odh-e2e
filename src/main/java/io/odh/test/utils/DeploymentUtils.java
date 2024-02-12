/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.utils;

import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodCondition;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentCondition;
import io.odh.test.TestConstants;
import io.odh.test.TestUtils;
import io.odh.test.framework.manager.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.util.Arrays.asList;

public class DeploymentUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeploymentUtils.class);
    private static final long READINESS_TIMEOUT = TestConstants.GLOBAL_TIMEOUT;
    private static final long DELETION_TIMEOUT = TestConstants.GLOBAL_TIMEOUT;

    private DeploymentUtils() { }

    /**
     * Log actual status of deployment with pods
     * @param deployment - every Deployment, that HasMetadata and has status (fabric8 status)
     **/
    public static void logCurrentDeploymentStatus(Deployment deployment, String namespaceName) {
        if (deployment != null) {
            String kind = deployment.getKind();
            String name = deployment.getMetadata().getName();

            List<String> log = new ArrayList<>(asList("\n", kind, " status:\n", "\nConditions:\n"));

            for (DeploymentCondition deploymentCondition : deployment.getStatus().getConditions()) {
                log.add("\tType: " + deploymentCondition.getType() + "\n");
                log.add("\tMessage: " + deploymentCondition.getMessage() + "\n");
            }

            if (!ResourceManager.getKubeClient().listPodsByPrefixInName(namespaceName, name).isEmpty()) {
                log.add("\nPods with conditions and messages:\n\n");

                for (Pod pod : ResourceManager.getKubeClient().listPodsByPrefixInName(namespaceName, name)) {
                    log.add(pod.getMetadata().getName() + ":");
                    for (PodCondition podCondition : pod.getStatus().getConditions()) {
                        if (podCondition.getMessage() != null) {
                            log.add("\n\tType: " + podCondition.getType() + "\n");
                            log.add("\tMessage: " + podCondition.getMessage() + "\n");
                        }
                    }
                    log.add("\n\n");
                }
                LOGGER.info("{}", String.join("", log));
            }

            LOGGER.info("{}", String.join("", log));
        }
    }

    public static boolean waitForDeploymentReady(String namespaceName, String deploymentName) {
        LOGGER.info("Waiting for Deployment: {}/{} to be ready", namespaceName, deploymentName);

        TestUtils.waitFor("readiness of Deployment: " + namespaceName + "/" + deploymentName,
            TestConstants.GLOBAL_POLL_INTERVAL_SHORT, READINESS_TIMEOUT,
            () -> ResourceManager.getKubeClient().getClient().apps().deployments().inNamespace(namespaceName).withName(deploymentName).isReady(),
            () -> DeploymentUtils.logCurrentDeploymentStatus(ResourceManager.getKubeClient().getDeployment(namespaceName, deploymentName), namespaceName));

        LOGGER.info("Deployment: {}/{} is ready", namespaceName, deploymentName);
        return true;
    }

    /**
     * Wait until the given Deployment has been deleted.
     * @param namespaceName Namespace name
     * @param name The name of the Deployment.
     */
    public static void waitForDeploymentDeletion(String namespaceName, String name) {
        LOGGER.debug("Waiting for Deployment: {}/{} deletion", namespaceName, name);
        TestUtils.waitFor("deletion of Deployment: " + namespaceName + "/" + name, TestConstants.GLOBAL_POLL_INTERVAL_MEDIUM, DELETION_TIMEOUT,
            () -> {
                if (ResourceManager.getKubeClient().getDeployment(namespaceName, name) == null) {
                    return true;
                } else {
                    LOGGER.warn("Deployment: {}/{} is not deleted yet! Triggering force delete by cmd client!", namespaceName, name);
                    ResourceManager.getKubeClient().getClient().apps().deployments().inNamespace(namespaceName).withName(name).delete();
                    return false;
                }
            });
        LOGGER.debug("Deployment: {}/{} was deleted", namespaceName, name);
    }

    /**
     * Returns a map of pod name to resource version for the Pods currently in the given deployment.
     * @param name The Deployment name.
     * @return A map of pod name to resource version for Pods in the given Deployment.
     */
    public static Map<String, String> depSnapshot(String namespaceName, String name) {
        Deployment deployment = ResourceManager.getKubeClient().getDeployment(namespaceName, name);
        LabelSelector selector = deployment.getSpec().getSelector();
        return PodUtils.podSnapshot(namespaceName, selector);
    }

    /**
     * Method to check that all Pods for expected Deployment were rolled
     * @param namespaceName Namespace name
     * @param name Deployment name
     * @param snapshot Snapshot of Pods for Deployment before the rolling update
     * @return true when the Pods for Deployment are recreated
     */
    public static boolean depHasRolled(String namespaceName, String name, Map<String, String> snapshot) {
        LOGGER.debug("Existing snapshot: {}/{}", namespaceName, new TreeMap<>(snapshot));
        Map<String, String> map = PodUtils.podSnapshot(namespaceName, ResourceManager.getKubeClient().getDeployment(namespaceName, name).getSpec().getSelector());
        LOGGER.debug("Current  snapshot: {}/{}", namespaceName, new TreeMap<>(map));
        int current = map.size();
        map.keySet().retainAll(snapshot.keySet());
        if (current == snapshot.size() && map.isEmpty()) {
            LOGGER.debug("All Pods seem to have rolled");
            return true;
        } else {
            LOGGER.debug("Some Pods still need to roll: {}/{}", namespaceName, map);
            return false;
        }
    }

    public static Map<String, String> waitTillDepHasRolled(String namespaceName, String deploymentName, Map<String, String> snapshot) {
        LOGGER.info("Waiting for Deployment: {}/{} rolling update", namespaceName, deploymentName);
        TestUtils.waitFor("rolling update of Deployment " + namespaceName + "/" + deploymentName,
                TestConstants.GLOBAL_POLL_INTERVAL_MEDIUM, TestConstants.GLOBAL_TIMEOUT,
                () -> depHasRolled(namespaceName, deploymentName, snapshot));

        return depSnapshot(namespaceName, deploymentName);
    }
}
