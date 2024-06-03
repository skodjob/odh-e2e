/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.framework.manager.resources;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.events.v1.Event;
import io.fabric8.kubernetes.client.dsl.EventingAPIGroupDSL;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.odh.test.OdhConstants;
import io.odh.test.TestConstants;
import io.odh.test.TestUtils;
import io.odh.test.utils.PodUtils;
import io.opendatahub.datasciencecluster.v1.DataScienceCluster;
import io.skodjob.testframe.interfaces.ResourceType;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.wait.Wait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class DataScienceClusterType implements ResourceType<DataScienceCluster> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataScienceClusterType.class);

    @Override
    public NonNamespaceOperation<?, ?, ?> getClient() {
        return dataScienceCLusterClient();
    }

    @Override
    public String getKind() {
        return "DataScienceCluster";
    }

    public DataScienceCluster get(String name) {
        return dataScienceCLusterClient().withName(name).get();
    }

    @Override
    public void create(DataScienceCluster resource) {
        if (get(resource.getMetadata().getName()) == null) {
            dataScienceCLusterClient().resource(resource).create();
        } else {
            update(resource);
        }
    }

    @Override
    public void update(DataScienceCluster resource) {
        dataScienceCLusterClient().resource(resource).update();
    }

    @Override
    public void delete(String s) {
        dataScienceCLusterClient().withName(s).delete();
    }

    @Override
    public void replace(String s, Consumer<DataScienceCluster> editor) {
        DataScienceCluster toBeUpdated = dataScienceCLusterClient().withName(s).get();
        editor.accept(toBeUpdated);
        update(toBeUpdated);
    }

    @Override
    public boolean waitForReadiness(DataScienceCluster resource) {
        String message = String.format("DataScienceCluster %s readiness", resource.getMetadata().getName());
        Wait.until(message, TestConstants.GLOBAL_POLL_INTERVAL_SHORT, TestConstants.GLOBAL_TIMEOUT, () -> {
            boolean dscReady;

            DataScienceCluster dsc = dataScienceCLusterClient().withName(resource.getMetadata().getName()).get();

            String dashboardStatus = TestUtils.getDscConditionByType(dsc.getStatus().getConditions(), "dashboardReady").getStatus();
            LOGGER.debug("DataScienceCluster {} Dashboard status: {}", resource.getMetadata().getName(), dashboardStatus);
            dscReady = dashboardStatus.equals("True");

            String workbenchesStatus = TestUtils.getDscConditionByType(dsc.getStatus().getConditions(), "workbenchesReady").getStatus();
            LOGGER.debug("DataScienceCluster {} Workbenches status: {}", resource.getMetadata().getName(), workbenchesStatus);
            dscReady = dscReady && workbenchesStatus.equals("True");

            // TODO uncomment once https://issues.redhat.com/browse/RHOAIENG-416 is fixed
//            // Wait for CodeFlare
//            if (resource.getSpec().getComponents().getCodeflare().getManagementState().equals(Codeflare.ManagementState.MANAGED)) {
//                String codeflareStatus = KubeUtils.getDscConditionByType(dsc.getStatus().getConditions(), "codeflareReady").getStatus();
//                LOGGER.debug("DataScienceCluster {} CodeFlare status: {}", resource.getMetadata().getName(), codeflareStatus);
//                dscReady = dscReady && codeflareStatus.equals("True");
//            }
//
//            // Wait for ModelMesh
//            if (resource.getSpec().getComponents().getModelmeshserving().getManagementState().equals(Modelmeshserving.ManagementState.MANAGED)) {
//                String modemeshStatus = KubeUtils.getDscConditionByType(dsc.getStatus().getConditions(), "model-meshReady").getStatus();
//                LOGGER.debug("DataScienceCluster {} ModelMesh status: {}", resource.getMetadata().getName(), modemeshStatus);
//                dscReady = dscReady && modemeshStatus.equals("True");
//            }
//
//            // Wait for Ray
//            if (resource.getSpec().getComponents().getRay().getManagementState().equals(Ray.ManagementState.MANAGED)) {
//                String rayStatus = KubeUtils.getDscConditionByType(dsc.getStatus().getConditions(), "rayReady").getStatus();
//                LOGGER.debug("DataScienceCluster {} Ray status: {}", resource.getMetadata().getName(), rayStatus);
//                dscReady = dscReady && rayStatus.equals("True");
//            }
//
//            // Wait for Kueue
//            if (resource.getSpec().getComponents().getKueue().getManagementState().equals(Kueue.ManagementState.MANAGED)) {
//                String kueueStatus = KubeUtils.getDscConditionByType(dsc.getStatus().getConditions(), "kueueReady").getStatus();
//                LOGGER.debug("DataScienceCluster {} Kueue status: {}", resource.getMetadata().getName(), kueueStatus);
//                dscReady = dscReady && kueueStatus.equals("True");
//            }
//
//            // Wait for KServe
//            if (resource.getSpec().getComponents().getKserve().getManagementState().equals(Kserve.ManagementState.MANAGED)) {
//                String kserveStatus = KubeUtils.getDscConditionByType(dsc.getStatus().getConditions(), "kserveReady").getStatus();
//                LOGGER.debug("DataScienceCluster {} KServe status: {}", resource.getMetadata().getName(), kserveStatus);
//                dscReady = dscReady && kserveStatus.equals("True");
//            }
//
//            // Wait for PipelinesOperator
//            if (resource.getSpec().getComponents().getDatasciencepipelines().getManagementState().equals(Datasciencepipelines.ManagementState.MANAGED)) {
//                String pipelinesStatus = KubeUtils.getDscConditionByType(dsc.getStatus().getConditions(), "data-science-pipelines-operatorReady").getStatus();
//                LOGGER.debug("DataScienceCluster {} DataSciencePipelines status: {}", resource.getMetadata().getName(), pipelinesStatus);
//                dscReady = dscReady && pipelinesStatus.equals("True");
//            }

            // Check that DSC reconciliation has been successfully finalized
            // https://github.com/red-hat-data-services/rhods-operator/blob/rhoai-2.8/controllers/datasciencecluster/datasciencecluster_controller.go#L257

            // Wait for standard Kubernetes condition types (status for the whole DSC)
            record ConditionExpectation(String conditionType, String expectedStatus) { }
            List<ConditionExpectation> conditionExpectations = List.of(
                    new ConditionExpectation("Available", "True"),
                    new ConditionExpectation("Progressing", "False"),
                    new ConditionExpectation("Degraded", "False"),
                    new ConditionExpectation("Upgradeable", "True")
            );
            for (ConditionExpectation conditionExpectation : conditionExpectations) {
                String conditionType = conditionExpectation.conditionType;
                String expectedStatus = conditionExpectation.expectedStatus;
                String conditionStatus = TestUtils.getDscConditionByType(dsc.getStatus().getConditions(), conditionType).getStatus();
                LOGGER.debug("DataScienceCluster {} {} status: {}", resource.getMetadata().getName(), conditionType, conditionStatus);
                dscReady = dscReady && Objects.equals(conditionStatus, expectedStatus);
            }

            // Wait for ReconcileComplete condition (for the whole DSC)
            String reconcileStatus = TestUtils.getDscConditionByType(dsc.getStatus().getConditions(), "ReconcileComplete").getStatus();
            LOGGER.debug("DataScienceCluster {} ReconcileComplete status: {}", resource.getMetadata().getName(), reconcileStatus);
            dscReady = dscReady && reconcileStatus.equals("True");

            // Wait for DataScienceClusterCreationSuccessful event
            EventingAPIGroupDSL eventsClient = KubeResourceManager.getKubeClient().getClient().events();
            List<Event> resourceEvents = eventsClient.v1().events().inAnyNamespace().withNewFilter()
                    .withField("regarding.name", resource.getMetadata().getName())
                    .withField("regarding.uid", resource.getMetadata().getUid())
                    .endFilter().list().getItems();
            LOGGER.debug("DataScienceCluster {} events: {}", resource.getMetadata().getName(), resourceEvents.stream().map(Event::getReason).toList());
            boolean hasCreationSuccessfulEvent = resourceEvents.stream()
                    .anyMatch(resourceEvent -> Objects.equals(resourceEvent.getReason(), OdhConstants.DSC_CREATION_SUCCESSFUL_EVENT_NAME));
            dscReady = dscReady && hasCreationSuccessfulEvent;

            return dscReady;
        }, () -> { });

        String namespace = OdhConstants.CONTROLLERS_NAMESPACE;
        LOGGER.info("Waiting for pods readiness in {}", namespace);
        PodUtils.waitForPodsReady(namespace, true, () -> {
            KubeResourceManager.getKubeCmdClient().inNamespace(namespace).exec(false, "get", "pods");
            KubeResourceManager.getKubeCmdClient().inNamespace(namespace).exec(false, "get", "events");
        });

        return true;
    }

    @Override
    public boolean waitForDeletion(DataScienceCluster dataScienceCluster) {
        return get(dataScienceCluster.getMetadata().getName()) == null;
    }

    public static MixedOperation<DataScienceCluster, KubernetesResourceList<DataScienceCluster>, Resource<DataScienceCluster>> dataScienceCLusterClient() {
        return KubeResourceManager.getKubeClient().getClient().resources(DataScienceCluster.class);
    }
}
