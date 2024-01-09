/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e;

import io.odh.test.framework.listeners.ResourceManagerContextHandler;
import io.odh.test.framework.listeners.TestVisualSeparator;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.framework.listeners.TestExceptionCallbackListener;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TestExceptionCallbackListener.class)
@ExtendWith(ResourceManagerContextHandler.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class Abstract implements TestVisualSeparator {

    static {
        ResourceManager.getInstance();
    }
}
