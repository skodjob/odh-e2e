/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e;

import io.odh.test.Environment;
import io.odh.test.TestConstants;
import io.odh.test.framework.listeners.TestExceptionCallbackListener;
import io.odh.test.framework.manager.requirements.AuthorinoOperator;
import io.odh.test.framework.manager.requirements.PipelinesOperator;
import io.odh.test.framework.manager.requirements.ServerlessOperator;
import io.odh.test.framework.manager.requirements.ServiceMeshOperator;
import io.odh.test.framework.manager.resources.DataScienceClusterType;
import io.odh.test.framework.manager.resources.DataScienceInitializationType;
import io.odh.test.framework.manager.resources.InferenceServiceType;
import io.odh.test.framework.manager.resources.NotebookType;
import io.skodjob.testframe.annotations.ResourceManager;
import io.skodjob.testframe.annotations.TestVisualSeparator;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.resources.NamespaceType;
import io.skodjob.testframe.resources.OperatorGroupType;
import io.skodjob.testframe.resources.SubscriptionType;
import io.skodjob.testframe.utils.KubeUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(TestExceptionCallbackListener.class)
@ResourceManager(cleanResources = false)
@TestVisualSeparator
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class Abstract {

    private static final Logger LOGGER = LoggerFactory.getLogger(Abstract.class);

    static {
        KubeResourceManager.getInstance().setResourceTypes(
                new NamespaceType(),
                new SubscriptionType(),
                new OperatorGroupType(),
                new DataScienceClusterType(),
                new DataScienceInitializationType(),
                new NotebookType(),
                new InferenceServiceType()
        );
        KubeResourceManager.getInstance().addCreateCallback(r -> {
            if (r.getKind().equals("Namespace")) {
                KubeUtils.labelNamespace(r.getMetadata().getName(), TestConstants.LOG_COLLECT_LABEL, "true");
            }
        });
    }

    @BeforeAll
    void setupDependencies() {
        if (Environment.SKIP_INSTALL_OPERATOR_DEPS) {
            LOGGER.info("Operator dependencies install is skipped");
            return;
        }
        PipelinesOperator.deployOperator();
        ServiceMeshOperator.deployOperator();
        ServerlessOperator.deployOperator();
        AuthorinoOperator.deployOperator();
    }
}
