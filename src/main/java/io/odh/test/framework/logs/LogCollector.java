/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.framework.logs;

import io.fabric8.kubernetes.api.model.Pod;
import io.odh.test.Environment;
import io.odh.test.OdhConstants;
import io.odh.test.TestUtils;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.platform.KubeClient;
import io.odh.test.platform.cmdClient.KubeCmdClient;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LogCollector {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogCollector.class);

    /**
     * Calls storing cluster info for connected cluster
     */
    public static void saveKubernetesState(ExtensionContext extensionContext, Throwable throwable) throws Throwable {
        Path logPath = TestUtils.getLogPath(Environment.LOG_DIR.resolve("failedTest").toString(), extensionContext);
        Files.createDirectories(logPath);
        LOGGER.info("Storing cluster info into {}", logPath);
        try {
            saveClusterState(logPath);
        } catch (IOException ex) {
            LOGGER.warn("Cannot save logs in {}", logPath);
        }
        throw throwable;
    }

    private static void writeLogsFromPods(Path logpath, Pod pod) {
        pod.getSpec().getContainers().forEach(container -> {
            try {
                LOGGER.debug("Get logs from pod {}/{} container {}", pod.getMetadata().getNamespace(), pod.getMetadata().getName(), container.getName());
                Files.writeString(logpath.resolve(pod.getMetadata().getNamespace() + "-" + pod.getMetadata().getName() + "-" + container.getName() + ".log"),
                        ResourceManager.getClient().getLogsFromContainer(pod.getMetadata().getNamespace(), pod.getMetadata().getName(), container.getName()));
            } catch (IOException e) {
                LOGGER.warn("Cannot get logs for pod {}/{}", pod.getMetadata().getNamespace(), pod.getMetadata().getName());
            }
        });
    }

    private static void saveClusterState(Path logpath) throws IOException {
        KubeClient kube = ResourceManager.getClient();
        KubeCmdClient cmdClient = ResourceManager.getKubeCmdClient();
        Files.writeString(logpath.resolve("describe-cluster-nodes.log"), cmdClient.exec(false, false, "describe", "nodes").out());
        Files.writeString(logpath.resolve("all-events.log"), cmdClient.exec(false, false, "get", "events", "--all-namespaces").out());
        Files.writeString(logpath.resolve("pvs.log"), cmdClient.exec(false, false, "describe", "pv").out());
        Files.writeString(logpath.resolve("dsc.yml"), cmdClient.exec(false, false, "get", "dsc", "-o", "yaml").out());
        Files.writeString(logpath.resolve("dsci.yml"), cmdClient.exec(false, false, "get", "dsci", "-o", "yaml").out());
        kube.listPodsByPrefixInName(OdhConstants.BUNDLE_OPERATOR_NAMESPACE, "opendatahub-operator-controller-manager").forEach(pod -> {
            writeLogsFromPods(logpath, pod);
        });
        kube.listPodsByPrefixInName(OdhConstants.OLM_OPERATOR_NAMESPACE, "opendatahub").forEach(pod -> {
            writeLogsFromPods(logpath, pod);
        });
        kube.listPods(OdhConstants.CONTROLLERS_NAMESPACE).forEach(pod -> {
            writeLogsFromPods(logpath, pod);
        });
    }
}
