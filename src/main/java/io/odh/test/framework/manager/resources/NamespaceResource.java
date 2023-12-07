/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.framework.manager.resources;

import io.fabric8.kubernetes.api.model.Namespace;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.framework.manager.ResourceType;


public class NamespaceResource implements ResourceType<Namespace> {

    @Override
    public String getKind() {
        return "Namespace";
    }

    @Override
    public Namespace get(String namespace, String name) {
        return ResourceManager.getClient().getClient().namespaces().withName(name).get();
    }

    @Override
    public void create(Namespace resource) {
        if (get("", resource.getMetadata().getName()) != null) {
            ResourceManager.getClient().getClient().resource(resource).update();
        } else {
            ResourceManager.getClient().getClient().resource(resource).create();
        }
    }

    @Override
    public void delete(Namespace resource) {
        ResourceManager.getClient().getClient().namespaces().withName(resource.getMetadata().getName()).delete();
    }

    @Override
    public void update(Namespace resource) {
        ResourceManager.getClient().getClient().resource(resource).update();
    }

    @Override
    public boolean waitForReadiness(Namespace resource) {
        return resource != null;
    }
}
