/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e;

import io.odh.test.framework.listeners.ResourceManagerContextHandler;
import io.odh.test.framework.listeners.TestVisualSeparator;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.framework.listeners.TestExceptionCallbackListener;
import io.odh.test.framework.manager.requirements.AuthorinoOperator;
import io.odh.test.framework.manager.requirements.PipelinesOperator;
import io.odh.test.framework.manager.requirements.ServerlessOperator;
import io.odh.test.framework.manager.requirements.ServiceMeshOperator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TestExceptionCallbackListener.class)
@ExtendWith(ResourceManagerContextHandler.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class Abstract implements TestVisualSeparator {

    static {
        ResourceManager.getInstance();
    }

    @BeforeAll
    void setupDependencies() {
        PipelinesOperator.deployOperator();
        ServiceMeshOperator.deployOperator();
        ServerlessOperator.deployOperator();
        AuthorinoOperator.deployOperator();
    }
}
