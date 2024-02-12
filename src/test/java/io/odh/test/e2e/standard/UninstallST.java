/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e.standard;

import java.util.Map;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.odh.test.Environment;
import io.odh.test.OdhConstants;
import io.odh.test.TestUtils;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.utils.DscUtils;
import io.odh.test.utils.NamespaceUtils;
import io.opendatahub.datasciencecluster.v1.DataScienceCluster;
import io.opendatahub.dscinitialization.v1.DSCInitialization;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

@Disabled("Disabled because of the https://issues.redhat.com/browse/RHOAIENG-499")
@DisabledIfEnvironmentVariable(
        named = Environment.SKIP_DEPLOY_DSCI_DSC_ENV,
        matches = "true",
        disabledReason = "Default DSCI and DSC deployed no need to run test")
public class UninstallST extends StandardAbstract {

    private static final String DS_PROJECT_NAME = "test-uninstall";
    private static final String DELETE_CONFIG_MAP_NAME = "delete-self-managed-odh";
    private static final String DELETE_ANNOTATION = "api.openshift.com/addon-managed-odh-delete";

    /**
     * Following the official Uninstallation steps for RHOAI:
     * <pre>
     * <a href="https://access.redhat.com/documentation/en-us/red_hat_openshift_ai_self-managed/2.5/html-single/installing_and_uninstalling_openshift_ai_self-managed/index#installing-openshift-ai-self-managed_uninstalling-openshift-ai-self-managed">Uninstalling Red Hat OpenShift AI Self-Managed by using the CLI</a>
     * </pre>
     *
     * Known issue <a href="https://issues.redhat.com/browse/RHOAIENG-499">RHOAIENG-499</a>
     */
    @Test
    void testUninstallSimpleScenario() {
        if (!ResourceManager.getKubeCmdClient().namespace(OdhConstants.OLM_OPERATOR_NAMESPACE)
                .list("configmap").contains(DELETE_CONFIG_MAP_NAME)) {
            ConfigMap cm = new ConfigMapBuilder()
                    .withNewMetadata()
                    .withName(DELETE_CONFIG_MAP_NAME)
                    .withNamespace(OdhConstants.OLM_OPERATOR_NAMESPACE)
                    .withAnnotations(Map.ofEntries(Map.entry(DELETE_ANNOTATION, "true")))
                    .endMetadata()
                    .build();
            ResourceManager.getInstance().createResourceWithWait(cm);
        } else {
            Assertions.fail(String.format("The configmap '%s' is present on the cluster before the uninstall test started!", DELETE_CONFIG_MAP_NAME));
        }

        // Now the product should start to uninstall, let's wait a bit and check the result.
        TestUtils.waitFor(String.format("the '%s' namespace to be removed as operator is being uninstalled",
                        OdhConstants.CONTROLLERS_NAMESPACE), 2000, 20000,
                () -> !ResourceManager.getKubeClient().namespaceExists(OdhConstants.CONTROLLERS_NAMESPACE));

        // Let's remove the operator namespace now
        ResourceManager.getKubeCmdClient().deleteNamespace(OdhConstants.OLM_OPERATOR_NAMESPACE);
        NamespaceUtils.waitForNamespaceDeletion(OdhConstants.OLM_OPERATOR_NAMESPACE);

        // Check that all other expected resources have been deleted
        Assertions.assertTrue(ResourceManager.getKubeCmdClient().namespace(OdhConstants.OLM_OPERATOR_NAMESPACE).list(
                "subscriptions").isEmpty(), "The operator subscription is still present!");
        Assertions.assertFalse(ResourceManager.getKubeClient().namespaceExists(OdhConstants.MONITORING_NAMESPACE),
                String.format("Namespace '%s' hasn't been removed by the operator uninstall operation!",
                        OdhConstants.MONITORING_NAMESPACE));
        Assertions.assertFalse(ResourceManager.getKubeClient().namespaceExists(OdhConstants.NOTEBOOKS_NAMESPACE),
                String.format("Namespace '%s' hasn't been removed by the operator uninstall operation!",
                        OdhConstants.NOTEBOOKS_NAMESPACE));

        // Following should be removed and we actually checked above already, so maybe remove from here TODO
        Assertions.assertFalse(ResourceManager.getKubeClient().namespaceExists(OdhConstants.CONTROLLERS_NAMESPACE),
                String.format("Namespace '%s' hasn't been removed by the operator uninstall operation!",
                        OdhConstants.CONTROLLERS_NAMESPACE));
        Assertions.assertFalse(ResourceManager.getKubeClient().namespaceExists(OdhConstants.OLM_OPERATOR_NAMESPACE),
                String.format("Namespace '%s' hasn't been removed!",
                        OdhConstants.OLM_OPERATOR_NAMESPACE));
    }

    @BeforeAll
    void deployDataScienceCluster() {
        // Create DSCI
        DSCInitialization dsci = DscUtils.getBasicDSCI();
        // Create DSC
        DataScienceCluster dsc = DscUtils.getBasicDSC(DS_PROJECT_NAME);

        // Deploy DSCI,DSC
        ResourceManager.getInstance().createResourceWithWait(dsci);
        ResourceManager.getInstance().createResourceWithWait(dsc);
    }
}
