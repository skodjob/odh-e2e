/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e;

import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.framework.TestExceptionCallbackListener;
import io.odh.test.framework.TestSeparator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

@DisplayNameGeneration(DisplayNameGenerator.IndicativeSentences.class)
@ExtendWith(TestExceptionCallbackListener.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Abstract implements TestSeparator {

    static {
        ResourceManager.getInstance();
    }

    @AfterAll
    void teardownEnvironment() {
        ResourceManager.getInstance().deleteResources();
    }
}
