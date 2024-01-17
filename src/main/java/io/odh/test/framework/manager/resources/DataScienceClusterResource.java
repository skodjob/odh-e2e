/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.framework.manager.resources;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.odh.test.OdhConstants;
import io.odh.test.TestConstants;
import io.odh.test.TestUtils;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.framework.manager.ResourceType;
import io.odh.test.platform.KubeUtils;
import io.odh.test.utils.PodUtils;
import io.opendatahub.datasciencecluster.v1.DataScienceCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataScienceClusterResource implements ResourceType<DataScienceCluster> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataScienceClusterResource.class);
    @Override
    public String getKind() {
        return "DataScienceCluster";
    }

    @Override
    public DataScienceCluster get(String namespace, String name) {
        return dataScienceCLusterClient().withName(name).get();
    }

    @Override
    public void create(DataScienceCluster resource) {
        dataScienceCLusterClient().resource(resource).create();
    }

    @Override
    public void delete(DataScienceCluster resource) {
        dataScienceCLusterClient().withName(resource.getMetadata().getName()).delete();
    }

    @Override
    public void update(DataScienceCluster resource) {
        dataScienceCLusterClient().resource(resource).update();
    }

    @Override
    public boolean waitForReadiness(DataScienceCluster resource) {
        String message = String.format("DataScienceCluster %s readiness", resource.getMetadata().getName());
        TestUtils.waitFor(message, TestConstants.GLOBAL_POLL_INTERVAL_SHORT, TestConstants.GLOBAL_TIMEOUT, () -> {
            boolean dscReady;

            DataScienceCluster dsc = dataScienceCLusterClient().withName(resource.getMetadata().getName()).get();

            String dashboardStatus = KubeUtils.getDscConditionByType(dsc.getStatus().getConditions(), "dashboardReady").getStatus();
            LOGGER.debug("DataScienceCluster {} dashboard status: {}", resource.getMetadata().getName(), dashboardStatus);
            dscReady = dashboardStatus.equals("True");

            String workbenchesStatus = KubeUtils.getDscConditionByType(dsc.getStatus().getConditions(), "workbenchesReady").getStatus();
            LOGGER.debug("DataScienceCluster {} workbenches status: {}", resource.getMetadata().getName(), workbenchesStatus);
            dscReady = dscReady && workbenchesStatus.equals("True");

            return dscReady;
        }, () -> { });

        String namespace = OdhConstants.CONTROLLERS_NAMESPACE;
        LOGGER.info("Waiting for pods readiness in {}", namespace);
        PodUtils.waitForPodsReady(namespace, true, () -> {
            ResourceManager.getKubeCmdClient().namespace(namespace).exec(false, "get", "pods");
            ResourceManager.getKubeCmdClient().namespace(namespace).exec(false, "get", "events");
        });

        return true;
    }

    public static MixedOperation<DataScienceCluster, KubernetesResourceList<DataScienceCluster>, Resource<DataScienceCluster>> dataScienceCLusterClient() {
        return ResourceManager.getClient().getClient().resources(DataScienceCluster.class);
    }

}
