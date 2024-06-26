/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.framework.manager.requirements;

import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.openshift.api.model.operatorhub.v1.OperatorGroupBuilder;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.Subscription;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.SubscriptionBuilder;
import io.odh.test.OdhAnnotationsLabels;
import io.odh.test.TestConstants;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.resources.ResourceItem;
import io.skodjob.testframe.utils.PodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

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
        KubeResourceManager.getInstance().createOrUpdateResourceWithWait(ns);
        //Create operator group for the operator
        if (KubeResourceManager.getKubeClient().getOpenShiftClient().operatorHub().operatorGroups()
                .inNamespace(OPERATOR_NAMESPACE).list().getItems().isEmpty()) {
            OperatorGroupBuilder operatorGroup = new OperatorGroupBuilder()
                    .editOrNewMetadata()
                    .withName("odh-group")
                    .withNamespace(OPERATOR_NAMESPACE)
                    .withLabels(Collections.singletonMap(OdhAnnotationsLabels.APP_LABEL_KEY, OdhAnnotationsLabels.APP_LABEL_VALUE))
                    .endMetadata();

            KubeResourceManager.getInstance().createResourceWithWait(operatorGroup.build());
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

        KubeResourceManager.getInstance().createOrUpdateResourceWithWait(subscription);
        KubeResourceManager.getInstance().pushToStack(new ResourceItem<>(() -> deleteOperator(ns), null));
        isOperatorReady();
    }

    public static void isOperatorReady() {
        PodUtils.waitForPodsReadyWithRestart(OPERATOR_NAMESPACE,
                new LabelSelectorBuilder().withMatchLabels(Map.of("name", "knative-operator")).build(), 1, true);
        PodUtils.waitForPodsReadyWithRestart(OPERATOR_NAMESPACE,
                new LabelSelectorBuilder().withMatchLabels(Map.of("name", "knative-openshift")).build(), 1, true);
    }

    public static void deleteOperator(Namespace namespace) {
        KubeResourceManager.getKubeClient().delete(Collections.singletonList(namespace));
    }
}
