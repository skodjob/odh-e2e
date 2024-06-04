/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.framework.manager.resources;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.odh.test.TestConstants;
import io.odh.test.TestUtils;
import io.opendatahub.dscinitialization.v1.DSCInitialization;
import io.skodjob.testframe.interfaces.ResourceType;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.wait.Wait;

import java.util.function.Consumer;

public class DataScienceInitializationType implements ResourceType<DSCInitialization> {

    @Override
    public NonNamespaceOperation<?, ?, ?> getClient() {
        return dsciClient();
    }

    @Override
    public String getKind() {
        return "DSCInitialization";
    }

    public DSCInitialization get(String name) {
        return dsciClient().withName(name).get();
    }

    @Override
    public void create(DSCInitialization resource) {
        if (get(resource.getMetadata().getName()) == null) {
            TestUtils.runUntilPass(5, () -> dsciClient().resource(resource).create());
        } else {
            TestUtils.runUntilPass(5, () -> dsciClient().resource(resource).update());
        }
    }

    @Override
    public void update(DSCInitialization resource) {
        TestUtils.runUntilPass(5, () -> dsciClient().resource(resource).update());
    }

    @Override
    public void delete(String s) {
        dsciClient().withName(s).delete();
    }

    @Override
    public void replace(String s, Consumer<DSCInitialization> editor) {
        DSCInitialization toBeUpdated = dsciClient().withName(s).get();
        editor.accept(toBeUpdated);
        update(toBeUpdated);
    }

    @Override
    public boolean waitForReadiness(DSCInitialization resource) {
        String message = String.format("DSCInitialization %s readiness", resource.getMetadata().getName());
        Wait.until(message, TestConstants.GLOBAL_POLL_INTERVAL_SHORT, TestConstants.GLOBAL_TIMEOUT, () -> {
            boolean dsciReady;

            DSCInitialization dsci = dsciClient().withName(resource.getMetadata().getName()).get();

            dsciReady = dsci.getStatus().getPhase().equals("Ready");

            return dsciReady;
        }, () -> {
        });
        return true;
    }

    @Override
    public boolean waitForDeletion(DSCInitialization dscInitialization) {
        return get(dscInitialization.getMetadata().getName()) == null;
    }

    public static MixedOperation<DSCInitialization, KubernetesResourceList<DSCInitialization>, Resource<DSCInitialization>> dsciClient() {
        return KubeResourceManager.getKubeClient().getClient().resources(DSCInitialization.class);
    }

}
