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

    boolean modifyOperatorImage = true;

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
        resources = ResourceManager.getKubeClient().readResourcesFromYaml(is);
    }

    public BundleInstall() throws IOException {
        //use default latest
        this(Environment.INSTALL_FILE_PATH);
    }

    public void disableModifyOperatorImage() {
        this.modifyOperatorImage = false;
    }

    public void enableModifyOperatorImage() {
        this.modifyOperatorImage = true;
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

    private void modifyOperatorImage() {
        if (Environment.OPERATOR_IMAGE_OVERRIDE != null && this.modifyOperatorImage) {
            for (HasMetadata r : resources) {
                if (r instanceof Deployment d) {
                    d.getSpec().getTemplate().getSpec().getContainers().get(0).setImage(Environment.OPERATOR_IMAGE_OVERRIDE);
                }
            }
        }
    }

    public void create() {
        modifyOperatorImage();
        ResourceManager.getInstance().createResourceWithWait(resources.toArray(new HasMetadata[0]));
        ResourceManager.getInstance().pushToStack(new ResourceItem<>(KubeUtils::deleteDefaultDSCI, null));
    }

    public void createWithoutResourceManager() {
        modifyOperatorImage();
        ResourceManager.getKubeClient().create(resources, r -> r);
    }

    public void deleteWithoutResourceManager() {
        KubeUtils.deleteDefaultDSCI();
        ResourceManager.getKubeClient().delete(resources);
    }
}
