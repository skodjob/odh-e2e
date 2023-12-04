/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.framework.manager.resources;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.operatorhub.v1.OperatorGroup;
import io.fabric8.openshift.api.model.operatorhub.v1.OperatorGroupList;
import io.fabric8.openshift.client.OpenShiftClient;
import io.odh.test.TestConstants;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.framework.manager.ResourceType;

public class OperatorGroupResource implements ResourceType<OperatorGroup> {
    @Override
    public String getKind() {
        return TestConstants.OPERATOR_GROUP;
    }

    @Override
    public OperatorGroup get(String namespace, String name) {
        return operatorGroupClient().inNamespace(namespace).withName(name).get();
    }

    @Override
    public void create(OperatorGroup resource) {
        operatorGroupClient().inNamespace(resource.getMetadata().getNamespace()).resource(resource).create();
    }

    @Override
    public void delete(OperatorGroup resource) {
        operatorGroupClient().inNamespace(resource.getMetadata().getNamespace()).withName(resource.getMetadata().getName()).delete();
    }

    @Override
    public void update(OperatorGroup resource) {
        operatorGroupClient().inNamespace(resource.getMetadata().getNamespace()).resource(resource).update();
    }

    @Override
    public boolean waitForReadiness(OperatorGroup resource) {
        return resource != null;
    }

    public static MixedOperation<OperatorGroup, OperatorGroupList, Resource<OperatorGroup>> operatorGroupClient() {
        return ResourceManager.getClient().getClient().adapt(OpenShiftClient.class).operatorHub().operatorGroups();
    }
}
