/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.framework.manager.requirements;

import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.Subscription;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.SubscriptionBuilder;
import io.odh.test.OdhAnnotationsLabels;
import io.odh.test.TestConstants;
import io.odh.test.framework.manager.ResourceItem;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.utils.PodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

public class ServiceMeshOperator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceMeshOperator.class);
    public static final String SUBSCRIPTION_NAME = "servicemeshoperator";
    public static final String OPERATOR_NAME = "servicemeshoperator";
    public static final String SERVICE_MESH_NAMESPACE = "istio-system";
    public static final String SERVICE_MESH_NAME = "data-science-smcp";

    public static void deployOperator() {
        Subscription subscription = new SubscriptionBuilder()
                .editOrNewMetadata()
                .withName(SUBSCRIPTION_NAME)
                .withNamespace(TestConstants.OPENSHIFT_OPERATORS_NS)
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
        ResourceManager.getInstance().pushToStack(new ResourceItem<>(() -> deleteOperator(subscription), null));
        isOperatorReady();
    }

    public static void isOperatorReady() {
        PodUtils.waitForPodsReadyWithRestart(TestConstants.OPENSHIFT_OPERATORS_NS,
                new LabelSelectorBuilder().withMatchLabels(Map.of("name", "istio-operator")).build(), 1, true);
    }

    public static void deleteOperator(Subscription subscription) {
        ResourceManager.getKubeClient().delete(Collections.singletonList(subscription));
    }
}
