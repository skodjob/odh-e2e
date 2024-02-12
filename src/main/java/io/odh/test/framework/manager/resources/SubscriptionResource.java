/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.framework.manager.resources;

import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.Subscription;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.SubscriptionList;
import io.fabric8.openshift.client.OpenShiftClient;
import io.odh.test.TestConstants;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.framework.manager.ResourceType;

public class SubscriptionResource implements ResourceType<Subscription> {

    @Override
    public String getKind() {
        return TestConstants.SUBSCRIPTION;
    }

    @Override
    public Subscription get(String namespace, String name) {
        return subscriptionClient().inNamespace(namespace).withName(name).get();
    }

    @Override
    public void create(Subscription resource) {
        subscriptionClient().inNamespace(resource.getMetadata().getNamespace()).resource(resource).create();
    }

    @Override
    public void delete(Subscription resource) {
        subscriptionClient().inNamespace(resource.getMetadata().getNamespace())
            .withName(resource.getMetadata().getName()).withPropagationPolicy(DeletionPropagation.FOREGROUND).delete();
    }

    @Override
    public void update(Subscription resource) {
        subscriptionClient().inNamespace(resource.getMetadata().getNamespace()).resource(resource).update();
    }

    @Override
    public boolean waitForReadiness(Subscription resource) {
        return resource != null;
    }

    public static MixedOperation<Subscription, SubscriptionList, Resource<Subscription>> subscriptionClient() {
        return ResourceManager.getKubeClient().getClient().adapt(OpenShiftClient.class).operatorHub().subscriptions();
    }
}
