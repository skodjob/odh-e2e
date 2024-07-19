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
import io.odh.test.utils.DscUtils;
import io.odh.test.utils.NamespaceUtils;
import io.opendatahub.datasciencecluster.v1.DataScienceCluster;
import io.opendatahub.dscinitialization.v1.DSCInitialization;
import io.skodjob.annotations.Contact;
import io.skodjob.annotations.Desc;
import io.skodjob.annotations.Step;
import io.skodjob.annotations.SuiteDoc;
import io.skodjob.annotations.TestDoc;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.wait.Wait;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuiteDoc(
    description = @Desc("Verifies that uninstall process removes all resources created by ODH installation"),
    beforeTestSteps = {
        @Step(value = "Deploy Pipelines Operator", expected = "Pipelines operator is available on the cluster"),
        @Step(value = "Deploy ServiceMesh Operator", expected = "ServiceMesh operator is available on the cluster"),
        @Step(value = "Deploy Serverless Operator", expected = "Serverless operator is available on the cluster"),
        @Step(value = "Install ODH operator", expected = "Operator is up and running and is able to serve it's operands"),
        @Step(value = "Deploy DSCI", expected = "DSCI is created and ready"),
        @Step(value = "Deploy DSC", expected = "DSC is created and ready")
    }
)
@DisabledIf(value = "isOdhTested", disabledReason = "This test needs to be modified to work on ODH.")
@DisabledIfEnvironmentVariable(
        named = Environment.SKIP_DEPLOY_DSCI_DSC_ENV,
        matches = "true",
        disabledReason = "Default DSCI and DSC deployed no need to run test")
public class UninstallST extends StandardAbstract {
    private static final Logger LOGGER = LoggerFactory.getLogger(UninstallST.class);

    private static final String DS_PROJECT_NAME = "test-uninstall";
    private static final String DELETE_CONFIG_MAP_NAME = "delete-self-managed-odh";
    private static final String DELETE_ANNOTATION = "api.openshift.com/addon-managed-odh-delete";

    /**
     * Following the official Uninstallation steps for RHOAI:
     * <pre>
     * <a href="https://access.redhat.com/documentation/en-us/red_hat_openshift_ai_self-managed/2-latest/html-single/installing_and_uninstalling_openshift_ai_self-managed/index#installing-openshift-ai-self-managed_uninstalling-openshift-ai-self-managed">Uninstalling Red Hat OpenShift AI Self-Managed by using the CLI</a>
     * </pre>
     *
     * Known issue <a href="https://issues.redhat.com/browse/RHOAIENG-499">RHOAIENG-499</a>
     */
    @TestDoc(
        description = @Desc("Check that user can create, run and deleted DataSciencePipeline from a DataScience project"),
        contact = @Contact(name = "Jan Stourac", email = "jstourac@redhat.com"),
        steps = {
            @Step(value = "Create uninstall configmap", expected = "ConfigMap exists"),
            @Step(value = "Wait for controllers namespace deletion", expected = "Controllers namespace is deleted"),
            @Step(value = "Check that relevant resources are deleted (Subscription, InstallPlan, CSV)", expected = "All relevant resources are deleted"),
            @Step(value = "Check that all related namespaces are deleted (monitoring, notebooks, controllers)", expected = "All related namespaces are deleted"),
            @Step(value = "Remove Operator namespace", expected = "Operator namespace is deleted")
        }
    )
    @Test
    void testUninstallSimpleScenario() {
        if (KubeResourceManager.getKubeCmdClient().inNamespace(OdhConstants.OLM_OPERATOR_NAMESPACE).list(
                "configmap").contains(DELETE_CONFIG_MAP_NAME)) {
            Assertions.fail(
                    String.format("The ConfigMap '%s' is present on the cluster before the uninstall test started!",
                            DELETE_CONFIG_MAP_NAME));
        }

        ConfigMap cm = new ConfigMapBuilder()
                .withNewMetadata()
                .withName(DELETE_CONFIG_MAP_NAME)
                .withNamespace(OdhConstants.OLM_OPERATOR_NAMESPACE)
                .withLabels(Map.ofEntries(Map.entry(DELETE_ANNOTATION, "true")))
                .endMetadata()
                .build();
        KubeResourceManager.getInstance().createResourceWithWait(cm);

        // Now the product should start to uninstall, let's wait a bit and check the result.
        Wait.until(String.format("the '%s' namespace to be removed as operator is being uninstalled",
                        OdhConstants.CONTROLLERS_NAMESPACE), 2000, 120_000,
                () -> !KubeResourceManager.getKubeClient().namespaceExists(OdhConstants.CONTROLLERS_NAMESPACE));

        // Operator itself should delete the CSV, Subscription and InstallPlan
        Assertions.assertTrue(KubeResourceManager.getKubeCmdClient().inNamespace(OdhConstants.OLM_OPERATOR_NAMESPACE).list(
                "subscriptions").isEmpty(), "The operator Subscription is still present!");
        Assertions.assertTrue(KubeResourceManager.getKubeCmdClient().inNamespace(OdhConstants.OLM_OPERATOR_NAMESPACE).list(
                "installplan").isEmpty(), "The operator InstallPlan is still present!");
        Assertions.assertFalse(KubeResourceManager.getKubeCmdClient().inNamespace(OdhConstants.OLM_OPERATOR_NAMESPACE).list(
                "csv").stream().anyMatch(s -> s.contains(OdhConstants.OLM_OPERATOR_NAME)),
                "The operator CSV is still present!");

        // TODO Check that the config map is deleted also
        // Looks like operator don't touch this.
        // This is deleted together with the operator namespace deletion for RHOAI, but what about ODH?
//        Assertions.assertFalse(ResourceManager.getKubeCmdClient().namespace(OdhConstants.OLM_OPERATOR_NAMESPACE).list("configmap").stream().anyMatch(s -> s.toString().equals(DELETE_CONFIG_MAP_NAME)),
//                "The operator deletion ConfigMap is still present!");

        // Let's remove the operator namespace now
        if (Environment.PRODUCT.equals(Environment.PRODUCT_ODH)) {
            LOGGER.info(String.format("Tested product is ODH - skipping removal of the '%s' namespace.", OdhConstants.OLM_OPERATOR_NAMESPACE));

            // In case of ODH the subscription, CSV and install plan aren't removed since we don't remove the namespace
            // as it is included in `openshift-operators` which is common namespaces for multiple other operators.
        } else {
            // Check that all other expected resources have been deleted
            Assertions.assertFalse(KubeResourceManager.getKubeClient().namespaceExists(OdhConstants.MONITORING_NAMESPACE),
                    String.format("Namespace '%s' hasn't been removed by the operator uninstall operation!",
                            OdhConstants.MONITORING_NAMESPACE));
            Assertions.assertFalse(KubeResourceManager.getKubeClient().namespaceExists(OdhConstants.NOTEBOOKS_NAMESPACE),
                    String.format("Namespace '%s' hasn't been removed by the operator uninstall operation!",
                            OdhConstants.NOTEBOOKS_NAMESPACE));

            KubeResourceManager.getKubeCmdClient().deleteNamespace(OdhConstants.OLM_OPERATOR_NAMESPACE);
            NamespaceUtils.waitForNamespaceDeletion(OdhConstants.OLM_OPERATOR_NAMESPACE);
        }
    }

    @BeforeAll
    void deployDataScienceCluster() {
        if (Environment.SKIP_DEPLOY_DSCI_DSC) {
            LOGGER.info("DSCI and DSC deploy is skipped");
            return;
        }
        // Create DSCI
        DSCInitialization dsci = DscUtils.getBasicDSCI();
        // Create DSC
        DataScienceCluster dsc = DscUtils.getBasicDSC(DS_PROJECT_NAME);

        // Deploy DSCI,DSC
        KubeResourceManager.getInstance().createOrUpdateResourceWithWait(dsci);
        KubeResourceManager.getInstance().createResourceWithWait(dsc);
    }

    static boolean isOdhTested() {
        return Environment.PRODUCT.equalsIgnoreCase(Environment.PRODUCT_ODH);
    }
}
