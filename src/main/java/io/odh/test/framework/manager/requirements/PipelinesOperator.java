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
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.resources.ResourceItem;
import io.skodjob.testframe.utils.PodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

public class PipelinesOperator {
    private static final Logger LOGGER = LoggerFactory.getLogger(PipelinesOperator.class);

    public static final String SUBSCRIPTION_NAME = "openshift-pipelines-operator";
    public static final String OPERATOR_NAME = "openshift-pipelines-operator-rh";

    public static void deployOperator() {
        Subscription subscription = new SubscriptionBuilder()
                .editOrNewMetadata()
                .withName(SUBSCRIPTION_NAME)
                .withNamespace(TestConstants.OPENSHIFT_OPERATORS_NS)
                .withLabels(Collections.singletonMap(OdhAnnotationsLabels.APP_LABEL_KEY, OdhAnnotationsLabels.APP_LABEL_VALUE))
                .endMetadata()
                .editOrNewSpec()
                .withName(OPERATOR_NAME)
                .withChannel(TestConstants.CHANNEL_LATEST)
                .withSource(TestConstants.REDHAT_CATALOG)
                .withSourceNamespace(TestConstants.OPENSHIFT_MARKETPLACE_NS)
                .withInstallPlanApproval(TestConstants.APPROVAL_AUTOMATIC)
                .editOrNewConfig()
                .endConfig()
                .endSpec()
                .build();

        KubeResourceManager.getInstance().createOrUpdateResourceWithWait(subscription);
        KubeResourceManager.getInstance().pushToStack(new ResourceItem<>(() -> deleteOperator(subscription), null));
        isOperatorReady();
    }

    public static void isOperatorReady() {
        PodUtils.waitForPodsReadyWithRestart(TestConstants.OPENSHIFT_OPERATORS_NS,
                new LabelSelectorBuilder().withMatchLabels(Map.of("app", "openshift-pipelines-operator")).build(), 1, true);
    }

    public static void deleteOperator(Subscription subscription) {
        KubeResourceManager.getKubeClient().delete(Collections.singletonList(subscription));
    }
}
