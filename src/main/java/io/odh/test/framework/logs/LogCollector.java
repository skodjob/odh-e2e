/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.framework.logs;

import io.fabric8.kubernetes.api.model.PodStatus;
import io.odh.test.Environment;
import io.odh.test.TestUtils;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.platform.cmdClient.KubeCmdClient;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class LogCollector {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogCollector.class);

    /**
     * Calls storing cluster info for connected cluster
     */
    public static void saveKubernetesState(ExtensionContext extensionContext, Throwable throwable) throws Throwable {
        LOGGER.warn("Printing all pods on cluster");
        ResourceManager.getClient().getClient().pods().inAnyNamespace().list().getItems().forEach(p ->
                LOGGER.info("Pod: {} in ns: {} with phase: {}",
                        p.getMetadata().getName(),
                        p.getMetadata().getNamespace(),
                        Optional.ofNullable(p.getStatus()).map(PodStatus::getPhase).orElse("null")));

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

    private static void saveClusterState(Path logpath) throws IOException {
        KubeCmdClient cmdClient = ResourceManager.getKubeCmdClient();
        Files.writeString(logpath.resolve("describe-cluster-nodes.log"), cmdClient.exec(false, false, "describe", "nodes").out());
        Files.writeString(logpath.resolve("all-events.log"), cmdClient.exec(false, false, "get", "events", "--all-namespaces").out());
        Files.writeString(logpath.resolve("pvs.log"), cmdClient.exec(false, false, "describe", "pv").out());
        Files.writeString(logpath.resolve("dsc.yml"), cmdClient.exec(false, false, "get", "dsc", "-o", "yaml").out());
        Files.writeString(logpath.resolve("dsci.yml"), cmdClient.exec(false, false, "get", "dsci", "-o", "yaml").out());
    }
}
