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
import io.odh.test.utils.PodUtils;
import io.skodjob.testframe.interfaces.NamespacedResourceType;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.wait.Wait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class InferenceServiceType implements NamespacedResourceType<InferenceService> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InferenceServiceType.class);

    @Override
    public String getKind() {
        return "InferenceService";
    }

    public InferenceService get(String namespace, String name) {
        return inferenceServiceClient().inNamespace(namespace).withName(name).get();
    }

    @Override
    public void create(InferenceService resource) {
        inferenceServiceClient().resource(resource).create();
    }

    @Override
    public void update(InferenceService resource) {
        inferenceServiceClient().resource(resource).update();
    }

    @Override
    public void delete(String resource) {
        inferenceServiceClient().withName(resource).delete();
    }

    @Override
    public void replace(String s, Consumer<InferenceService> editor) {
        InferenceService toBeUpdated = inferenceServiceClient().withName(s).get();
        editor.accept(toBeUpdated);
        update(toBeUpdated);
    }

    @Override
    public boolean waitForReadiness(InferenceService resource) {
        String message = String.format("InferenceService %s readiness", resource.getMetadata().getName());
        Wait.until(message, TestConstants.GLOBAL_POLL_INTERVAL_SHORT, TestConstants.GLOBAL_TIMEOUT, () -> {
            boolean isReady;

            InferenceService inferenceService = get(resource.getMetadata().getNamespace(), resource.getMetadata().getName());

            String predictorReadyStatus = TestUtils.getInferenceServiceConditionByType(inferenceService.getStatus().getConditions(), "PredictorReady").getStatus();
            LOGGER.debug("InferenceService {} PredictorReady status: {}", resource.getMetadata().getName(), predictorReadyStatus);
            isReady = predictorReadyStatus.equals("True");

            String readyStatus = TestUtils.getInferenceServiceConditionByType(inferenceService.getStatus().getConditions(), "Ready").getStatus();
            LOGGER.debug("InferenceService {} Ready status: {}", resource.getMetadata().getName(), readyStatus);
            isReady = isReady && readyStatus.equals("True");

            return isReady;
        }, () -> { });

        String namespace = resource.getMetadata().getNamespace();
        LOGGER.info("Waiting for pods readiness in {}", namespace);
        PodUtils.waitForPodsReady(namespace, true, () -> {
            KubeResourceManager.getKubeCmdClient().inNamespace(namespace).exec(false, "get", "pods");
            KubeResourceManager.getKubeCmdClient().inNamespace(namespace).exec(false, "get", "events");
        });

        return true;
    }

    @Override
    public boolean waitForDeletion(InferenceService inferenceService) {
        return get(inferenceService.getMetadata().getNamespace(), inferenceService.getMetadata().getName()) == null;
    }

    public static MixedOperation<InferenceService, KubernetesResourceList<InferenceService>, Resource<InferenceService>> inferenceServiceClient() {
        return KubeResourceManager.getKubeClient().getClient().resources(InferenceService.class);
    }

    @Override
    public MixedOperation<?, ?, ?> getClient() {
        return inferenceServiceClient();
    }

    @Override
    public void createInNamespace(String namespace, InferenceService inferenceService) {
        inferenceServiceClient().inNamespace(namespace).resource(inferenceService).create();
    }

    @Override
    public void updateInNamespace(String namespace, InferenceService inferenceService) {
        inferenceServiceClient().inNamespace(namespace).resource(inferenceService).update();
    }

    @Override
    public void deleteFromNamespace(String namespace, String resource) {
        inferenceServiceClient().inNamespace(namespace).withName(resource).delete();
    }

    @Override
    public void replaceInNamespace(String namespace, String s, Consumer<InferenceService> editor) {
        InferenceService toBeUpdated = inferenceServiceClient().inNamespace(namespace).withName(s).get();
        editor.accept(toBeUpdated);
        update(toBeUpdated);
    }
}
