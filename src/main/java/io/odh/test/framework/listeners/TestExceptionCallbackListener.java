/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.framework.listeners;

import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.odh.test.Environment;
import io.odh.test.OdhConstants;
import io.odh.test.TestConstants;
import io.odh.test.TestUtils;
import io.skodjob.testframe.LogCollector;
import io.skodjob.testframe.LogCollectorBuilder;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.utils.KubeUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

/**
 * jUnit5 specific class which listening on test exception callbacks
 */
public class TestExceptionCallbackListener implements TestExecutionExceptionHandler, LifecycleMethodExecutionExceptionHandler {
    static final Logger LOGGER = LoggerFactory.getLogger(TestExceptionCallbackListener.class);

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        LOGGER.error("Test failed at {} : {}", "Test execution", throwable.getMessage(), throwable);
        saveKubernetesState(context, throwable);
    }

    @Override
    public void handleBeforeAllMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        LOGGER.error("Test failed at {} : {}", "Test before all", throwable.getMessage(), throwable);
        saveKubernetesState(context, throwable);
    }

    @Override
    public void handleBeforeEachMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        LOGGER.error("Test failed at {} : {}", "Test before each", throwable.getMessage(), throwable);
        saveKubernetesState(context, throwable);
    }

    @Override
    public void handleAfterEachMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        LOGGER.error("Test failed at {} : {}", "Test after each", throwable.getMessage(), throwable);
        saveKubernetesState(context, throwable);
    }

    @Override
    public void handleAfterAllMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        LOGGER.error("Test failed at {} : {}", "Test after all", throwable.getMessage(), throwable);
        saveKubernetesState(context, throwable);
    }

    private void saveKubernetesState(ExtensionContext context, Throwable throwable) throws Throwable {
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
        LogCollector logCollector = new LogCollectorBuilder()
                .withNamespacedResources("secret", "deployment", "subscription", "operatorgroup", "configmaps")
                .withClusterWideResources("dsci", "dsc", "nodes")
                .withKubeClient(KubeResourceManager.getKubeClient())
                .withKubeCmdClient(KubeResourceManager.getKubeCmdClient())
                .withRootFolderPath(TestUtils.getLogPath(
                        Environment.LOG_DIR.resolve("failedTest").toString(), context).toString())
                .build();
        logCollector.collectFromNamespacesWithLabels(new LabelSelectorBuilder()
                .withMatchLabels(Collections.singletonMap(TestConstants.LOG_COLLECT_LABEL, "true"))
                .build());
        logCollector.collectClusterWideResources();
        throw throwable;
    }
}
