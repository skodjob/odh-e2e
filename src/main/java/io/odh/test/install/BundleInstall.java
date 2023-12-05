/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.install;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.odh.test.Environment;
import io.odh.test.TestConstants;
import io.odh.test.TestUtils;
import io.odh.test.framework.manager.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class BundleInstall {

    private static final Logger LOGGER = LoggerFactory.getLogger(BundleInstall.class);

    List<HasMetadata> resources;

    public BundleInstall(String installFilePath) throws IOException {
        InputStream is;
        if (installFilePath.equals(TestConstants.LATEST_BUNDLE_DEPLOY_FILE)
                || installFilePath.equals(TestConstants.RELEASED_BUNDLE_DEPLOY_FILE)) {
            is = TestUtils.getFileFromResourceAsStream(installFilePath);
        } else {
            is = new FileInputStream(installFilePath);
        }
        resources = ResourceManager.getClient().readResourcesFromYaml(is);
    }

    public BundleInstall() throws IOException {
        //use default latest
        this(Environment.INSTALL_FILE_PATH);
    }

    public void printResources() {
        resources.forEach(r -> {
            LOGGER.info("Kind: {}, Name: {}", r.getKind(), r.getMetadata().getName());
        });
    }

    public void installBundle() {
        //TODO implement using RM
    }
}
