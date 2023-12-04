/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.utils;

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

            if (!ResourceManager.getClient().listPodsByPrefixInName(namespaceName, name).isEmpty()) {
                log.add("\nPods with conditions and messages:\n\n");

                for (Pod pod : ResourceManager.getClient().listPodsByPrefixInName(namespaceName, name)) {
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
            () -> ResourceManager.getClient().getClient().apps().deployments().inNamespace(namespaceName).withName(deploymentName).isReady(),
            () -> DeploymentUtils.logCurrentDeploymentStatus(ResourceManager.getClient().getDeployment(namespaceName, deploymentName), namespaceName));

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
        TestUtils.waitFor("deletion of Deployment: " + namespaceName + "/" + name, TestConstants.GLOBAL_POLL_INTERVAL, DELETION_TIMEOUT,
            () -> {
                if (ResourceManager.getClient().getDeployment(namespaceName, name) == null) {
                    return true;
                } else {
                    LOGGER.warn("Deployment: {}/{} is not deleted yet! Triggering force delete by cmd client!", namespaceName, name);
                    ResourceManager.getClient().getClient().apps().deployments().inNamespace(namespaceName).withName(name).delete();
                    return false;
                }
            });
        LOGGER.debug("Deployment: {}/{} was deleted", namespaceName, name);
    }
}
