/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e;

import io.odh.test.framework.ExtensionContextParameterResolver;
import io.odh.test.framework.TestCallbackListener;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.framework.TestExceptionCallbackListener;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

@DisplayNameGeneration(DisplayNameGenerator.IndicativeSentences.class)
@ExtendWith(TestExceptionCallbackListener.class)
@ExtendWith(TestCallbackListener.class)
@ExtendWith(ExtensionContextParameterResolver.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Abstract {

    static {
        ResourceManager.getInstance();
    }
}
