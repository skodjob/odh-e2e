/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.install;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.odh.test.Environment;
import io.odh.test.TestConstants;
import io.odh.test.TestUtils;
import io.odh.test.framework.manager.ResourceItem;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.platform.KubeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class BundleInstall {

    private static final Logger LOGGER = LoggerFactory.getLogger(BundleInstall.class);

    List<HasMetadata> resources;
    File installFile;

    public BundleInstall(String installFilePath) throws IOException {
        InputStream is;
        if (installFilePath.equals(TestConstants.LATEST_BUNDLE_DEPLOY_FILE)
                || installFilePath.equals(TestConstants.RELEASED_BUNDLE_DEPLOY_FILE)) {
            installFile = new File("src/main/resources/" + installFilePath);
            is = TestUtils.getFileFromResourceAsStream(installFilePath);
        } else {
            installFile = new File(installFilePath);
            is = new FileInputStream(installFilePath);
        }
        resources = ResourceManager.getClient().readResourcesFromYaml(is);
    }

    public BundleInstall() throws IOException {
        //use default latest
        this(Environment.INSTALL_FILE_PATH);
    }

    public String getNamespace() {
        return resources.stream().filter(r -> r instanceof Namespace).findFirst().get().getMetadata().getName();
    }

    public String getDeploymentName() {
        return resources.stream().filter(r -> r instanceof Deployment).findFirst().get().getMetadata().getName();
    }

    public String getDeploymentImage() {
        return ((Deployment) resources.stream().filter(r -> r instanceof Deployment).findFirst().get())
                .getSpec().getTemplate().getSpec().getContainers()
                .stream().filter(c -> c.getName().equals("manager")).findFirst().get().getImage();
    }

    public void printResources() {
        resources.forEach(r -> {
            LOGGER.info("Kind: {}, Name: {}", r.getKind(), r.getMetadata().getName());
        });
    }

    public void create() {
        ResourceManager.getInstance().createResourceWithWait(resources.toArray(new HasMetadata[0]));
        ResourceManager.getInstance().pushToStack(new ResourceItem(KubeUtils::deleteDefaultDSCI, null));
    }

    public void createWithoutResourceManager() throws IOException {
        ResourceManager.getKubeCmdClient().namespace(getNamespace()).apply(installFile.getAbsolutePath());
    }

    public void deleteWithoutResourceManager() throws IOException {
        KubeUtils.deleteDefaultDSCI();
        ResourceManager.getKubeCmdClient().namespace(getNamespace()).delete(installFile.getAbsolutePath());
    }
}
