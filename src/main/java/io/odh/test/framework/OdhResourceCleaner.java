/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.framework;

import io.odh.test.platform.KubeUtils;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OdhResourceCleaner implements AfterAllCallback {

    static final Logger LOGGER = LoggerFactory.getLogger(OdhResourceCleaner.class);

    static final String SEPARATOR_CHAR = "#";

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        KubeUtils.clearOdhCRDs();
    }
}
