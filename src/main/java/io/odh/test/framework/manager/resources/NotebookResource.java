/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.framework.manager.resources;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.client.OpenShiftClient;
import io.odh.test.Environment;
import io.odh.test.OdhConstants;
import io.odh.test.TestUtils;
import io.skodjob.testframe.interfaces.NamespacedResourceType;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.kubeflow.v1.Notebook;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;


public class NotebookResource implements NamespacedResourceType<Notebook> {

    private static final String REGISTRY_PATH = "image-registry.openshift-image-registry.svc:5000";
    public static final String JUPYTER_MINIMAL_IMAGE = "jupyter-minimal-notebook";
    public static final String JUPYTER_MINIMAL_2023_2_TAG = "2023.2";
    private static final Map<String, String> ODH_IMAGES_MAP;

    static {
        ODH_IMAGES_MAP = Map.<String, String>of(JUPYTER_MINIMAL_IMAGE, "jupyter-minimal-notebook");
    }

    private static final Map<String, String> RHOAI_IMAGES_MAP;

    static {
        RHOAI_IMAGES_MAP = Map.<String, String>of(JUPYTER_MINIMAL_IMAGE, "s2i-minimal-notebook");
    }
    private static final String NOTEBOOK_TEMPLATE_PATH = "notebook.yaml";
    @Override
    public String getKind() {
        return "Notebook";
    }

    public Notebook get(String namespace, String name) {
        return notebookClient().inNamespace(namespace).withName(name).get();
    }

    @Override
    public void create(Notebook resource) {
        notebookClient().inNamespace(resource.getMetadata().getNamespace()).resource(resource).create();
    }

    @Override
    public void update(Notebook resource) {
        notebookClient().inNamespace(resource.getMetadata().getNamespace()).resource(resource).update();
    }

    @Override
    public void delete(String s) {
        notebookClient().withName(s).delete();
    }

    @Override
    public void replace(String resource, Consumer<Notebook> editor) {
        Notebook toBeUpdated = notebookClient().withName(resource).get();
        editor.accept(toBeUpdated);
        update(toBeUpdated);
    }

    @Override
    public boolean waitForReadiness(Notebook resource) {
        return resource != null;
    }

    @Override
    public boolean waitForDeletion(Notebook notebook) {
        return get(notebook.getMetadata().getNamespace(), notebook.getMetadata().getName()) == null;
    }

    public static MixedOperation<Notebook, KubernetesResourceList<Notebook>, Resource<Notebook>> notebookClient() {
        return KubeResourceManager.getKubeClient().getClient().resources(Notebook.class);
    }

    public static Notebook loadDefaultNotebook(String namespace, String name, String image) throws IOException {
        InputStream is = TestUtils.getFileFromResourceAsStream(NOTEBOOK_TEMPLATE_PATH);
        String notebookString = IOUtils.toString(is, "UTF-8");
        notebookString = notebookString.replace("my-project", namespace).replace("my-workbench", name);
        // Set new Route url
        String routeHost = KubeResourceManager.getKubeClient().getClient().adapt(OpenShiftClient.class).routes().inNamespace(OdhConstants.CONTROLLERS_NAMESPACE).withName(OdhConstants.DASHBOARD_ROUTE_NAME).get().getSpec().getHost();
        notebookString = notebookString.replace("odh_dashboard_route", "https://" + routeHost);
        // Set correct username
        String username = KubeResourceManager.getKubeCmdClient().getUsername().strip();
        notebookString = notebookString.replace("odh_user", username);
        // Replace image
        notebookString = notebookString.replace("notebook_image_placeholder", image);

        return TestUtils.configFromYaml(notebookString, Notebook.class);
    }

    public static String getNotebookImage(String imageName, String imageTag) {
        if (Objects.equals(Environment.PRODUCT, Environment.PRODUCT_ODH)) {
            return REGISTRY_PATH + "/" + OdhConstants.CONTROLLERS_NAMESPACE + "/" + ODH_IMAGES_MAP.get(imageName) + ":" + imageTag;
        } else {
            return REGISTRY_PATH + "/" + OdhConstants.CONTROLLERS_NAMESPACE + "/" + RHOAI_IMAGES_MAP.get(imageName) + ":" + imageTag;
        }
    }

    @Override
    public MixedOperation<?, ?, ?> getClient() {
        return notebookClient();
    }

    @Override
    public void createInNamespace(String namespace, Notebook notebook) {
        notebookClient().inNamespace(namespace).resource(notebook).create();
    }

    @Override
    public void updateInNamespace(String namespace, Notebook notebook) {
        notebookClient().inNamespace(namespace).resource(notebook).update();
    }

    @Override
    public void deleteFromNamespace(String namespace, String resource) {
        notebookClient().inNamespace(namespace).withName(resource).delete();
    }

    @Override
    public void replaceInNamespace(String namespace, String resoruce, Consumer<Notebook> editor) {
        Notebook toBeUpdated = notebookClient().inNamespace(namespace).withName(resoruce).get();
        editor.accept(toBeUpdated);
        update(toBeUpdated);
    }
}
