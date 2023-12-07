/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e.upgrade;

import io.odh.test.e2e.Abstract;
import io.odh.test.framework.manager.ResourceManager;
import org.junit.jupiter.api.AfterAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpgradeAbstract extends Abstract {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeAbstract.class);

    @AfterAll
    void teardownEnvironment() {
        ResourceManager.getInstance().deleteResources();
    }
}
