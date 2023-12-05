/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e;

import io.odh.test.framework.ExtensionContextParameterResolver;
import io.odh.test.framework.TestCallbackListener;
import io.odh.test.Environment;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.framework.TestExceptionCallbackListener;
import io.odh.test.install.InstallTypes;
import io.odh.test.install.OlmInstall;
import io.odh.test.framework.TestSeparator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayNameGeneration(DisplayNameGenerator.IndicativeSentences.class)
@ExtendWith(TestExceptionCallbackListener.class)
@ExtendWith(TestCallbackListener.class)
@ExtendWith(ExtensionContextParameterResolver.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Abstract {

    static {
        ResourceManager.getInstance();
    }

    @BeforeAll
    void setupEnvironment() {
        if (Environment.OPERATOR_INSTALL_TYPE.equals(InstallTypes.OLM.toString())) {
            OlmInstall olmInstall = new OlmInstall();
            olmInstall.create();
        } else if (Environment.OPERATOR_INSTALL_TYPE.equals(InstallTypes.BUNDLE.toString())) {
            LOGGER.error("Bundle install is not implemented yet!");
            assertTrue(false);
        } else {
            LOGGER.error("Unknown install type {}! You should implement it at first!", Environment.OPERATOR_INSTALL_TYPE);
            assertTrue(false);
        }
    }

    @AfterAll
    void teardownEnvironment() {
        ResourceManager.getInstance().deleteResources();
    }
}
