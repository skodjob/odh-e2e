/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.framework.manager.resources;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.kserve.serving.v1beta1.InferenceService;
import io.odh.test.TestConstants;
import io.odh.test.TestUtils;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.framework.manager.ResourceType;
import io.odh.test.platform.KubeUtils;
import io.odh.test.utils.PodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InferenceServiceResource implements ResourceType<InferenceService> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InferenceServiceResource.class);
    @Override
    public String getKind() {
        return "InferenceService";
    }

    @Override
    public InferenceService get(String namespace, String name) {
        return inferenceServiceClient().inNamespace(namespace).withName(name).get();
    }

    @Override
    public void create(InferenceService resource) {
        inferenceServiceClient().resource(resource).create();
    }

    @Override
    public void delete(InferenceService resource) {
        inferenceServiceClient().inNamespace(resource.getMetadata().getNamespace()).withName(resource.getMetadata().getName()).delete();
    }

    @Override
    public void update(InferenceService resource) {
        inferenceServiceClient().resource(resource).update();
    }

    @Override
    public boolean waitForReadiness(InferenceService resource) {
        String message = String.format("InferenceService %s readiness", resource.getMetadata().getName());
        TestUtils.waitFor(message, TestConstants.GLOBAL_POLL_INTERVAL_SHORT, TestConstants.GLOBAL_TIMEOUT, () -> {
            boolean isReady;

            InferenceService inferenceService = get(resource.getMetadata().getNamespace(), resource.getMetadata().getName());

            String predictorReadyStatus = KubeUtils.getInferenceServiceConditionByType(inferenceService.getStatus().getConditions(), "PredictorReady").getStatus();
            LOGGER.debug("InferenceService {} PredictorReady status: {}", resource.getMetadata().getName(), predictorReadyStatus);
            isReady = predictorReadyStatus.equals("True");

            String readyStatus = KubeUtils.getInferenceServiceConditionByType(inferenceService.getStatus().getConditions(), "Ready").getStatus();
            LOGGER.debug("InferenceService {} Ready status: {}", resource.getMetadata().getName(), readyStatus);
            isReady = isReady && readyStatus.equals("True");

            return isReady;
        }, () -> { });

        String namespace = resource.getMetadata().getNamespace();
        LOGGER.info("Waiting for pods readiness in {}", namespace);
        PodUtils.waitForPodsReady(namespace, true, () -> {
            ResourceManager.getKubeCmdClient().namespace(namespace).exec(false, "get", "pods");
            ResourceManager.getKubeCmdClient().namespace(namespace).exec(false, "get", "events");
        });

        return true;
    }

    public static MixedOperation<InferenceService, KubernetesResourceList<InferenceService>, Resource<InferenceService>> inferenceServiceClient() {
        return ResourceManager.getKubeClient().getClient().resources(InferenceService.class);
    }

}
