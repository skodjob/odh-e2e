/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e.standard;

import io.odh.test.Environment;
import io.odh.test.e2e.Abstract;
import io.odh.test.framework.listeners.OdhResourceCleaner;
import io.odh.test.framework.listeners.ResourceManagerDeleteHandler;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.install.BundleInstall;
import io.odh.test.install.InstallTypes;
import io.odh.test.install.OlmInstall;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.fail;

@Tag("standard")
@ExtendWith(OdhResourceCleaner.class)
@ExtendWith(ResourceManagerDeleteHandler.class)
public class StandardAbstract extends Abstract {

    private static final Logger LOGGER = LoggerFactory.getLogger(StandardAbstract.class);

    @BeforeAll
    void setupEnvironment() throws IOException {
        if (Environment.OPERATOR_INSTALL_TYPE.toLowerCase(Locale.ENGLISH)
                .equals(InstallTypes.OLM.toString().toLowerCase(Locale.ENGLISH))) {
            OlmInstall olmInstall = new OlmInstall();
            olmInstall.create();
        } else if (Environment.OPERATOR_INSTALL_TYPE.toLowerCase(Locale.ENGLISH)
                .equals(InstallTypes.BUNDLE.toString().toLowerCase(Locale.ENGLISH))) {
            BundleInstall bundleInstall = new BundleInstall();
            bundleInstall.create();
        } else {
            LOGGER.error("Unknown install type {}! You should implement it at first!", Environment.OPERATOR_INSTALL_TYPE);
            fail(String.format("Unknown install type %s! You should implement it at first!", Environment.OPERATOR_INSTALL_TYPE));
        }
    }

    @AfterAll
    void teardownEnvironment() {
        ResourceManager.getInstance().deleteResources();
    }
}
