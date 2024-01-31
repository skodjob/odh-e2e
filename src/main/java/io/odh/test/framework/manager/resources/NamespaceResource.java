/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.framework.manager.resources;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.odh.test.TestConstants;
import io.odh.test.TestUtils;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.framework.manager.ResourceType;

public class NamespaceResource implements ResourceType<Namespace> {

    @Override
    public String getKind() {
        return "Namespace";
    }

    @Override
    public Namespace get(String namespace, String name) {
        return ResourceManager.getKubeClient().getClient().namespaces().withName(name).get();
    }

    @Override
    public void create(Namespace resource) {
        if (get("", resource.getMetadata().getName()) != null) {
            ResourceManager.getKubeClient().getClient().resource(resource).update();
        } else {
            ResourceManager.getKubeClient().getClient().resource(resource).create();
        }
    }

    @Override
    public void delete(Namespace resource) {
        ResourceManager.getKubeClient().getClient().namespaces().withName(resource.getMetadata().getName()).delete();
    }

    @Override
    public void update(Namespace resource) {
        ResourceManager.getKubeClient().getClient().resource(resource).update();
    }

    @Override
    public boolean waitForReadiness(Namespace resource) {
        return resource != null;
    }

    public static void labelNamespace(String namespace, String key, String value) {
        if (ResourceManager.getKubeClient().namespaceExists(namespace)) {
            TestUtils.waitFor(String.format("Namespace %s has label: %s", namespace, TestConstants.LOG_COLLECT_LABEL), TestConstants.GLOBAL_POLL_INTERVAL_1_SEC, TestConstants.GLOBAL_STABILITY_TIME * 1000, () -> {
                try {
                    ResourceManager.getKubeClient().getClient().namespaces().withName(namespace).edit(n ->
                            new NamespaceBuilder(n)
                                    .editMetadata()
                                    .addToLabels(key, value)
                                    .endMetadata()
                                    .build());
                } catch (Exception ex) {
                    return false;
                }
                Namespace n = ResourceManager.getKubeClient().getClient().namespaces().withName(namespace).get();
                if (n != null) {
                    return n.getMetadata().getLabels().get(key) != null;
                }
                return false;
            });
        }
    }
}
