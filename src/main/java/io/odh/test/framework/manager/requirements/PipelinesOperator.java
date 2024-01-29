/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.framework.manager.requirements;

import io.fabric8.openshift.api.model.operatorhub.v1alpha1.Subscription;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.SubscriptionBuilder;
import io.odh.test.OdhAnnotationsLabels;
import io.odh.test.framework.manager.ResourceItem;
import io.odh.test.framework.manager.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;

public class PipelinesOperator {
    private static final Logger LOGGER = LoggerFactory.getLogger(PipelinesOperator.class);

    public static void deployOperator() {
        Subscription subscription = new SubscriptionBuilder()
                .editOrNewMetadata()
                .withName("openshift-pipelines-operator")
                .withNamespace("openshift-operators")
                .withLabels(Collections.singletonMap(OdhAnnotationsLabels.APP_LABEL_KEY, OdhAnnotationsLabels.APP_LABEL_VALUE))
                .endMetadata()
                .editOrNewSpec()
                .withName("openshift-pipelines-operator-rh")
                .withChannel("latest")
                .withSource("redhat-operators")
                .withSourceNamespace("openshift-marketplace")
                .withInstallPlanApproval("Automatic")
                .editOrNewConfig()
                .endConfig()
                .endSpec()
                .build();

        ResourceManager.getInstance().createResourceWithWait(subscription);
        ResourceManager.getInstance().pushToStack(new ResourceItem(() -> deleteOperator(subscription), null));
    }

    public static void deleteOperator(Subscription subscription) {
        ResourceManager.getClient().delete(Arrays.asList(subscription));
    }
}
