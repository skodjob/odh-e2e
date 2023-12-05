/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.framework.manager.resources;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.framework.manager.ResourceType;
import io.opendatahub.datasciencecluster.v1.DataScienceCluster;

public class DataScienceClusterResource implements ResourceType<DataScienceCluster> {
    @Override
    public String getKind() {
        return "DataScienceCluster";
    }

    @Override
    public DataScienceCluster get(String namespace, String name) {
        return dataScienceCLusterClient().inNamespace(namespace).withName(name).get();
    }

    @Override
    public void create(DataScienceCluster resource) {
        dataScienceCLusterClient().inNamespace(resource.getMetadata().getNamespace()).resource(resource).create();
    }

    @Override
    public void delete(DataScienceCluster resource) {
        dataScienceCLusterClient().inNamespace(resource.getMetadata().getNamespace()).withName(resource.getMetadata().getName()).delete();
    }

    @Override
    public void update(DataScienceCluster resource) {
        dataScienceCLusterClient().inNamespace(resource.getMetadata().getNamespace()).resource(resource).update();
    }

    @Override
    public boolean waitForReadiness(DataScienceCluster resource) {
        return resource != null;
    }

    public static MixedOperation<DataScienceCluster, KubernetesResourceList<DataScienceCluster>, Resource<DataScienceCluster>> dataScienceCLusterClient() {
        return ResourceManager.getClient().getClient().resources(DataScienceCluster.class);
    }

}
