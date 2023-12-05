/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.install;

import io.fabric8.openshift.api.model.operatorhub.v1.OperatorGroupBuilder;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.Subscription;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.SubscriptionBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.odh.test.Environment;
import io.odh.test.framework.manager.ResourceItem;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.framework.manager.resources.OperatorGroupResource;
import io.odh.test.utils.DeploymentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class OlmInstall {
    private static final Logger LOGGER = LoggerFactory.getLogger(OlmInstall.class);

    private String namespace = Environment.OLM_OPERATOR_NAMESPACE;
    private String channel = Environment.OLM_OPERATOR_CHANNEL;
    private String name = Environment.OLM_OPERATOR_NAME;
    private String operatorName = Environment.OLM_OPERATOR_NAME;
    private String sourceName = Environment.OLM_SOURCE_NAME;
    private String sourceNamespace = Environment.OLM_SOURCE_NAMESPACE;
    private String startingCsv;
    private String deploymentName = Environment.OLM_OPERATOR_DEPLOYMENT_NAME;
    private String olmAppBundlePrefix  = Environment.OLM_OPERATOR_NAME;
    private String operatorVersion  = Environment.OLM_OPERATOR_VERSION;
    private String csvName = operatorName + ".v" + operatorVersion;

    public void create() {
        createOperatorGroup();
        ResourceManager.RESOURCE_STACK.push(new ResourceItem(this::deleteCSV));
        createAndModifySubscription();

        // Wait for operator creation
        DeploymentUtils.waitForDeploymentReady(namespace, deploymentName);
    }

    /**
     * Creates OperatorGroup in specific namespace
     */
    private void createOperatorGroup() {
        if (OperatorGroupResource.operatorGroupClient().inNamespace(namespace).list().getItems().isEmpty()) {
            OperatorGroupBuilder operatorGroup = new OperatorGroupBuilder()
                .editOrNewMetadata()
                .withName("odh-group")
                .withNamespace(namespace)
                .withLabels(Collections.singletonMap("app", "odh"))
                .endMetadata();

            ResourceManager.getInstance().createResourceWithWait(operatorGroup.build());
        } else {
            LOGGER.info("OperatorGroup is already exists.");
        }
    }

    /**
     * Creates Subscription with spec from OlmConfiguration
     */

    private void createAndModifySubscription() {
        Subscription subscription = prepareSubscription();

        ResourceManager.getInstance().createResourceWithWait(subscription);
//        ResourceManager.RESOURCE_STACK.push(new ResourceItem(this::deleteCSV));

    }
    public void updateSubscription() {
        Subscription subscription = prepareSubscription();
        ResourceManager.getInstance().updateResource(subscription);
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
            .withInstallPlanApproval("Automatic")
            .editOrNewConfig()
            .endConfig()
            .endSpec()
            .build();
    }

    public void deleteCSV() {
        LOGGER.info("Deleting CSV {}/{}", namespace, olmAppBundlePrefix);
        ResourceManager.getClient().getClient().adapt(OpenShiftClient.class).operatorHub().clusterServiceVersions().inNamespace(namespace).withName(csvName).delete();
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
}
