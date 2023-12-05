/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e.standard;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.odh.test.e2e.Abstract;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.install.OlmInstall;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class OdhInstall extends Abstract {

    @Test
    void testInstallOdh() {
        OlmInstall olmInstall = new OlmInstall();
        olmInstall.create();

        Deployment dep = ResourceManager.getClient().getDeployment(olmInstall.getNamespace(), olmInstall.getDeploymentName());
        assertNotNull(dep);
    }
}
