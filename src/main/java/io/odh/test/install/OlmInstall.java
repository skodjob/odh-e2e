/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.install;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.openshift.api.model.operatorhub.v1.OperatorGroupBuilder;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.Subscription;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.SubscriptionBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.odh.test.Environment;
import io.odh.test.OdhConstants;
import io.odh.test.TestConstants;
import io.odh.test.TestUtils;
import io.odh.test.utils.DeploymentUtils;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.resources.ResourceItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class OlmInstall {
    private static final Logger LOGGER = LoggerFactory.getLogger(OlmInstall.class);

    private String namespace = OdhConstants.OLM_OPERATOR_NAMESPACE;
    private String channel = Environment.OLM_OPERATOR_CHANNEL;
    private String name = Environment.OLM_OPERATOR_NAME;
    private String operatorName = Environment.OLM_OPERATOR_NAME;
    private String sourceName = Environment.OLM_SOURCE_NAME;
    private String sourceNamespace = Environment.OLM_SOURCE_NAMESPACE;
    private String startingCsv;
    private String deploymentName = OdhConstants.OLM_OPERATOR_DEPLOYMENT_NAME;
    private String olmAppBundlePrefix  = Environment.OLM_OPERATOR_NAME;
    private String operatorVersion  = Environment.OLM_OPERATOR_VERSION;
    private String csvName = operatorName + "." + operatorVersion;

    private String approval = TestConstants.APPROVAL_AUTOMATIC;

    public void create() {
        createNamespace();
        // Create operator group and subscription
        createOperatorGroup();
        KubeResourceManager.getInstance().pushToStack(new ResourceItem<>(this::deleteCSV));
        createAndModifySubscription();

        // Wait for operator creation
        DeploymentUtils.waitForDeploymentReady(namespace, deploymentName);
    }

    public void createManual() {
        createNamespace();
        createOperatorGroup();
        KubeResourceManager.getInstance().pushToStack(new ResourceItem<>(this::deleteCSV));
        createAndModifySubscription();
    }

    /**
     * Creates namespace for operator-group and subscription
     */
    private void createNamespace() {
        if (!KubeResourceManager.getKubeClient().namespaceExists(namespace)) {
            // Create a namespace first because operator-group and subscription could use specific namespace
            Namespace ns = new NamespaceBuilder()
                .withNewMetadata()
                .withName(namespace)
                .endMetadata()
                .build();
            KubeResourceManager.getInstance().createResourceWithWait(ns);
        }
    }

    /**
     * Creates OperatorGroup in specific namespace
     */
    private void createOperatorGroup() {
        if (KubeResourceManager.getKubeClient().getOpenShiftClient().operatorHub().operatorGroups()
                .inNamespace(namespace).list().getItems().isEmpty()) {
            OperatorGroupBuilder operatorGroup = new OperatorGroupBuilder()
                .editOrNewMetadata()
                .withName("odh-group")
                .withNamespace(namespace)
                .withLabels(Collections.singletonMap("app", "odh"))
                .endMetadata();

            KubeResourceManager.getInstance().createResourceWithWait(operatorGroup.build());
        } else {
            LOGGER.info("OperatorGroup is already exists.");
        }
    }

    /**
     * Creates Subscription with spec from OlmConfiguration
     */

    private void createAndModifySubscription() {
        Subscription subscription = prepareSubscription();

        KubeResourceManager.getInstance().pushToStack(new ResourceItem<>(TestUtils::clearOdhRemainingResources, null));
        KubeResourceManager.getInstance().createOrUpdateResourceWithWait(subscription);
        KubeResourceManager.getInstance().pushToStack(new ResourceItem<>(TestUtils::deleteDefaultDSCI, null));
    }
    public void updateSubscription() {
        Subscription subscription = prepareSubscription();
        KubeResourceManager.getInstance().updateResource(subscription);
    }

    public Subscription prepareSubscription() {
        return new SubscriptionBuilder()
            .editOrNewMetadata()
            .withName(name)
            .withNamespace(namespace)
            .withLabels(Collections.singletonMap("app", "odh"))
            .endMetadata()
            .editOrNewSpec()
            .withName(operatorName)
            .withSource(sourceName)
            .withSourceNamespace(sourceNamespace)
            .withChannel(channel)
            .withStartingCSV(startingCsv)
            .withInstallPlanApproval(approval)
            .editOrNewConfig()
            .endConfig()
            .endSpec()
            .build();
    }

    public void deleteCSV() {
        KubeResourceManager.getKubeClient().getClient().adapt(OpenShiftClient.class).operatorHub().clusterServiceVersions().inNamespace(namespace)
            .list().getItems().stream().filter(csv -> csv.getMetadata().getName().contains(olmAppBundlePrefix)).toList()
            .forEach(csv -> {
                LOGGER.info("Deleting CSV {}", csv.getMetadata().getName());
                KubeResourceManager.getKubeClient().getOpenShiftClient().operatorHub().clusterServiceVersions().resource(csv).delete();
            });
        deleteInstallPlans();
    }

    public void deleteInstallPlans() {
        KubeResourceManager.getKubeClient().getClient().adapt(OpenShiftClient.class).operatorHub().installPlans().inNamespace(namespace)
            .list().getItems().stream().filter(ip -> ip.getSpec().getClusterServiceVersionNames().stream().toList().toString().contains(olmAppBundlePrefix)).toList()
            .forEach(ip -> {
                LOGGER.info("Deleting InstallPlan {}", ip.getMetadata().getName());
                KubeResourceManager.getKubeClient().getOpenShiftClient().operatorHub().installPlans().resource(ip).delete();
            });
    }

    /**
     * Useful getters for possible changes in upgrade tests
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getStartingCsv() {
        return startingCsv;
    }

    public void setStartingCsv(String startingCsv) {
        this.startingCsv = startingCsv;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    public String getOperatorVersion() {
        return operatorVersion;
    }

    public void setOperatorVersion(String operatorVersion) {
        this.operatorVersion = operatorVersion;
    }

    public String getCsvName() {
        return csvName;
    }

    public void setCsvName(String csvName) {
        this.csvName = csvName;
    }

    public String getApproval() {
        return approval;
    }

    public void setApproval(String approval) {
        this.approval = approval;
    }
}
