/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.framework.manager.requirements;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.openshift.api.model.operatorhub.v1.OperatorGroupBuilder;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.Subscription;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.SubscriptionBuilder;
import io.odh.test.OdhAnnotationsLabels;
import io.odh.test.TestConstants;
import io.odh.test.framework.manager.ResourceItem;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.framework.manager.resources.OperatorGroupResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;

public class ServerlessOperator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerlessOperator.class);
    public static final String SUBSCRIPTION_NAME = "serverless-operator";
    public static final String OPERATOR_NAME = "serverless-operator";
    public static final String OPERATOR_NAMESPACE = "openshift-serverless";
    public static void deployOperator() {
        // Create ns for the operator
        Namespace ns = new NamespaceBuilder()
                .withNewMetadata()
                .withName(OPERATOR_NAMESPACE)
                .withLabels(Collections.singletonMap(OdhAnnotationsLabels.APP_LABEL_KEY, OdhAnnotationsLabels.APP_LABEL_VALUE))
                .endMetadata()
                .build();
        ResourceManager.getInstance().createResourceWithoutWait(ns);
        //Create operator group for the operator
        if (OperatorGroupResource.operatorGroupClient().inNamespace(OPERATOR_NAMESPACE).list().getItems().isEmpty()) {
            OperatorGroupBuilder operatorGroup = new OperatorGroupBuilder()
                    .editOrNewMetadata()
                    .withName("odh-group")
                    .withNamespace(OPERATOR_NAMESPACE)
                    .withLabels(Collections.singletonMap(OdhAnnotationsLabels.APP_LABEL_KEY, OdhAnnotationsLabels.APP_LABEL_VALUE))
                    .endMetadata();

            ResourceManager.getInstance().createResourceWithWait(operatorGroup.build());
        } else {
            LOGGER.info("OperatorGroup is already exists.");
        }
        // Create subscription
        Subscription subscription = new SubscriptionBuilder()
                .editOrNewMetadata()
                .withName(SUBSCRIPTION_NAME)
                .withNamespace(OPERATOR_NAMESPACE)
                .withLabels(Collections.singletonMap(OdhAnnotationsLabels.APP_LABEL_KEY, OdhAnnotationsLabels.APP_LABEL_VALUE))
                .endMetadata()
                .editOrNewSpec()
                .withName(OPERATOR_NAME)
                .withChannel(TestConstants.CHANNEL_STABLE)
                .withSource(TestConstants.REDHAT_CATALOG)
                .withSourceNamespace(TestConstants.OPENSHIFT_MARKETPLACE_NS)
                .withInstallPlanApproval(TestConstants.APPROVAL_AUTOMATIC)
                .editOrNewConfig()
                .endConfig()
                .endSpec()
                .build();

        ResourceManager.getInstance().createResourceWithWait(subscription);
        ResourceManager.getInstance().pushToStack(new ResourceItem(() -> deleteOperator(ns), null));
    }

    public static void deleteOperator(Namespace namespace) {
        ResourceManager.getClient().delete(Arrays.asList(namespace));
    }
}
