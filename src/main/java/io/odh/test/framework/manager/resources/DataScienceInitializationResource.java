/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.framework.manager.resources;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.odh.test.TestConstants;
import io.odh.test.TestUtils;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.framework.manager.ResourceType;
import io.opendatahub.dscinitialization.v1.DSCInitialization;

public class DataScienceInitializationResource implements ResourceType<DSCInitialization> {

    @Override
    public String getKind() {
        return "DSCInitialization";
    }

    @Override
    public DSCInitialization get(String namespace, String name) {
        return dsciClient().withName(name).get();
    }

    @Override
    public void create(DSCInitialization resource) {
        dsciClient().resource(resource).create();
    }

    @Override
    public void delete(DSCInitialization resource) {
        dsciClient().withName(resource.getMetadata().getName()).delete();
    }

    @Override
    public void update(DSCInitialization resource) {
        dsciClient().resource(resource).update();
    }

    @Override
    public boolean waitForReadiness(DSCInitialization resource) {
        String message = String.format("DataScienceCluster %s readiness", resource.getMetadata().getName());
        TestUtils.waitFor(message, TestConstants.GLOBAL_POLL_INTERVAL_SHORT, TestConstants.GLOBAL_TIMEOUT, () -> {
            boolean dsciReady;

            DSCInitialization dsci = dsciClient().withName(resource.getMetadata().getName()).get();

            dsciReady = dsci.getStatus().getPhase().equals("Ready");

            return dsciReady;
        }, () -> {
        });
        return true;
    }

    public static MixedOperation<DSCInitialization, KubernetesResourceList<DSCInitialization>, Resource<DSCInitialization>> dsciClient() {
        return ResourceManager.getClient().getClient().resources(DSCInitialization.class);
    }

}
