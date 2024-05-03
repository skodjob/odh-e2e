/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.framework.logs;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.odh.test.Environment;
import io.odh.test.OdhConstants;
import io.odh.test.TestConstants;
import io.odh.test.TestUtils;
import io.skodjob.testframe.clients.KubeClient;
import io.skodjob.testframe.clients.cmdClient.KubeCmdClient;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.utils.KubeUtils;
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
        try {
            KubeUtils.labelNamespace(OdhConstants.BUNDLE_OPERATOR_NAMESPACE, TestConstants.LOG_COLLECT_LABEL, "true");
            KubeUtils.labelNamespace(OdhConstants.OLM_OPERATOR_NAMESPACE, TestConstants.LOG_COLLECT_LABEL, "true");
            KubeUtils.labelNamespace(OdhConstants.CONTROLLERS_NAMESPACE, TestConstants.LOG_COLLECT_LABEL, "true");
            KubeUtils.labelNamespace(OdhConstants.MONITORING_NAMESPACE, TestConstants.LOG_COLLECT_LABEL, "true");
            KubeUtils.labelNamespace(OdhConstants.ISTIO_SYSTEM_NAMESPACE, TestConstants.LOG_COLLECT_LABEL, "true");
            KubeUtils.labelNamespace(OdhConstants.KNATIVE_SERVING_NAMESPACE, TestConstants.LOG_COLLECT_LABEL, "true");
        } catch (Exception ignored) {
            LOGGER.warn("Cannot label namespaces for collect logs");
        }
        LOGGER.info("Storing cluster info into {}", logPath);
        try {
            saveClusterState(logPath);
        } catch (IOException ex) {
            LOGGER.warn("Cannot save logs in {}", logPath);
        }
        throw throwable;
    }

    private static void writeLogsFromPods(Path logpath, Pod pod) {
        try {
            Files.createDirectories(logpath.resolve(pod.getMetadata().getNamespace()));
        } catch (IOException e) {
            LOGGER.warn("Cannot create logdir in {}", logpath);
        }
        pod.getSpec().getContainers().forEach(container -> {
            try {
                LOGGER.debug("Get logs from pod {}/{} container {}", pod.getMetadata().getNamespace(), pod.getMetadata().getName(), container.getName());
                Files.writeString(logpath.resolve(pod.getMetadata().getNamespace()).resolve(pod.getMetadata().getName() + "-" + container.getName() + ".log"),
                        KubeResourceManager.getKubeClient().getLogsFromContainer(pod.getMetadata().getNamespace(), pod.getMetadata().getName(), container.getName()));
            } catch (Exception e) {
                LOGGER.warn("Cannot get logs for pod {}/{}", pod.getMetadata().getNamespace(), pod.getMetadata().getName());
            }
        });
    }

    private static void writePodsDescription(Path logpath, Pod pod) {
        try {
            Files.createDirectories(logpath.resolve(pod.getMetadata().getNamespace()));
        } catch (IOException e) {
            LOGGER.warn("Cannot create logdir in {}", logpath);
        }
        try {
            LOGGER.debug("Get description of pod {}/{}", pod.getMetadata().getNamespace(), pod.getMetadata().getName());
            Files.writeString(logpath.resolve(pod.getMetadata().getNamespace()).resolve(pod.getMetadata().getName() + ".describe.log"),
                    KubeResourceManager.getKubeCmdClient().namespace(pod.getMetadata().getNamespace()).describe(pod.getKind(), pod.getMetadata().getName()));
        } catch (Exception e) {
            LOGGER.warn("Cannot get description of pod {}/{}", pod.getMetadata().getNamespace(), pod.getMetadata().getName());
        }
    }

    private static void writeDeployments(Path logpath, Deployment deployment) {
        try {
            Files.createDirectories(logpath.resolve(deployment.getMetadata().getNamespace()));
        } catch (IOException e) {
            LOGGER.warn("Cannot create logdir in {}", logpath);
        }
        try {
            LOGGER.debug("Get deployment {}/{}", deployment.getMetadata().getNamespace(), deployment.getMetadata().getName());
            Files.writeString(logpath.resolve(deployment.getMetadata().getNamespace()).resolve("deployment-" + deployment.getMetadata().getName() + ".yaml"),
                    KubeResourceManager.getKubeCmdClient().exec(false, false, "get", "deployment", deployment.getMetadata().getName(),
                            "-n", deployment.getMetadata().getNamespace(), "-o", "yaml").out());
        } catch (Exception e) {
            LOGGER.warn("Cannot get deployment of pod {}/{}", deployment.getMetadata().getNamespace(), deployment.getMetadata().getName());
        }
    }

    private static void saveClusterState(Path logpath) throws IOException {
        KubeClient kube = KubeResourceManager.getKubeClient();
        KubeCmdClient<?> cmdClient = KubeResourceManager.getKubeCmdClient();

        // Collecting cluster wide resources and CRs
        Files.writeString(logpath.resolve("describe-cluster-nodes.log"), cmdClient.exec(false, false, "describe", "nodes").out());
        Files.writeString(logpath.resolve("namespaces.log"), cmdClient.exec(false, false, "get", "ns").out());
        Files.writeString(logpath.resolve("pods.log"), cmdClient.exec(false, false, "get", "po", "--all-namespaces").out());
        Files.writeString(logpath.resolve("all-events.log"), cmdClient.exec(false, false, "get", "events", "--all-namespaces").out());
        Files.writeString(logpath.resolve("pvs.log"), cmdClient.exec(false, false, "describe", "pv").out());
        Files.writeString(logpath.resolve("dsc.yml"), cmdClient.exec(false, false, "get", "dsc", "-o", "yaml").out());
        Files.writeString(logpath.resolve("dsci.yml"), cmdClient.exec(false, false, "get", "dsci", "-o", "yaml").out());
        Files.writeString(logpath.resolve("subscriptions.yml"), cmdClient.exec(false, false, "get", "subscriptions.operators.coreos.com", "--all-namespaces", "-o", "yaml").out());
        Files.writeString(logpath.resolve("notebooks.yml"), cmdClient.exec(false, false, "get", "notebook", "--all-namespaces", "-o", "yaml").out());

        kube.getClient().namespaces().withLabel(TestConstants.LOG_COLLECT_LABEL).list().getItems().forEach(ns -> {
            LOGGER.debug("Listing pods in {}", ns.getMetadata().getName());
            kube.getClient().pods().inNamespace(ns.getMetadata().getName()).list().getItems().forEach(pod -> {
                writeLogsFromPods(logpath, pod);
                writePodsDescription(logpath, pod);
            });
            LOGGER.debug("Listing deployments in {}", ns.getMetadata().getName());
            kube.getClient().apps().deployments().inNamespace(ns.getMetadata().getName()).list().getItems().forEach(d -> writeDeployments(logpath, d));
        });
    }
}
