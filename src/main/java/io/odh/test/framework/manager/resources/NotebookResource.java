/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.framework.manager.resources;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.client.OpenShiftClient;
import io.odh.test.OdhConstants;
import io.odh.test.TestUtils;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.framework.manager.ResourceType;
import org.kubeflow.v1.Notebook;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;


public class NotebookResource implements ResourceType<Notebook> {

    private static final String NOTEBOOK_TEMPLATE_PATH = "notebook.yaml";
    @Override
    public String getKind() {
        return "Notebook";
    }

    @Override
    public Notebook get(String namespace, String name) {
        return notebookClient().inNamespace(namespace).withName(name).get();
    }

    @Override
    public void create(Notebook resource) {
        notebookClient().inNamespace(resource.getMetadata().getNamespace()).resource(resource).create();
    }

    @Override
    public void delete(Notebook resource) {
        notebookClient().inNamespace(resource.getMetadata().getNamespace()).withName(resource.getMetadata().getName()).delete();
    }

    @Override
    public void update(Notebook resource) {
        notebookClient().inNamespace(resource.getMetadata().getNamespace()).resource(resource).update();
    }

    @Override
    public boolean waitForReadiness(Notebook resource) {
        return resource != null;
    }

    public static MixedOperation<Notebook, KubernetesResourceList<Notebook>, Resource<Notebook>> notebookClient() {
        return ResourceManager.getClient().getClient().resources(Notebook.class);
    }

    public static Notebook loadDefaultNotebook(String namespace, String name) throws IOException {
        InputStream is = TestUtils.getFileFromResourceAsStream(NOTEBOOK_TEMPLATE_PATH);
        String notebookString = IOUtils.toString(is, "UTF-8");
        notebookString = notebookString.replace("my-project", namespace).replace("my-workbench", name);
        // Set new Route url
        String routeHost = ResourceManager.getClient().getClient().adapt(OpenShiftClient.class).routes().inNamespace(OdhConstants.CONTROLLERS_NAMESPACE).withName(OdhConstants.DASHBOARD_ROUTE_NAME).get().getSpec().getHost();
        notebookString = notebookString.replace("odh_dashboard_route", "https://" + routeHost);
        // Set correct username
        String username = ResourceManager.getKubeCmdClient().getUsername().strip();
        notebookString = notebookString.replace("odh_user", username);

        return TestUtils.configFromYaml(notebookString, Notebook.class);
    }
}
