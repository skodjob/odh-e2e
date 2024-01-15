/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.platform;

import io.fabric8.openshift.api.model.operatorhub.v1alpha1.InstallPlan;
import io.odh.test.TestConstants;
import io.odh.test.TestUtils;
import io.odh.test.framework.manager.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.NoSuchElementException;

public class KubeUtils {

    static final Logger LOGGER = LoggerFactory.getLogger(KubeUtils.class);

    public static io.opendatahub.datasciencecluster.v1.datascienceclusterstatus.Conditions getDscConditionByType(List<io.opendatahub.datasciencecluster.v1.datascienceclusterstatus.Conditions> conditions, String type) {
        return conditions.stream().filter(c -> c.getType().equals(type)).findFirst().orElseGet(null);
    }

    public static org.kubeflow.v1.notebookstatus.Conditions getNotebookConditionByType(List<org.kubeflow.v1.notebookstatus.Conditions> conditions, String type) {
        return conditions.stream().filter(c -> c.getType().equals(type)).findFirst().orElseGet(null);
    }

    public static void clearOdhRemainingResources() {
        ResourceManager.getClient().getClient().apiextensions().v1().customResourceDefinitions().list().getItems()
            .stream().filter(crd -> crd.getMetadata().getName().contains("opendatahub.io")).toList()
            .forEach(crd -> {
                LOGGER.info("Deleting CRD {}", crd.getMetadata().getName());
                ResourceManager.getClient().getClient().resource(crd).delete();
            });
        ResourceManager.getClient().getClient().namespaces().withName("opendatahub").delete();
    }

    /**
     * TODO - this should be removed when https://github.com/opendatahub-io/opendatahub-operator/issues/765 will be resolved
     */
    public static void deleteDefaultDSCI() {
        LOGGER.info("Clearing DSCI ...");
        ResourceManager.getKubeCmdClient().exec(false, "delete", "dsci", "--all");
    }

    public static void waitForInstallPlan(String namespace, String csvName) {
        TestUtils.waitFor("Install plan with new version", TestConstants.GLOBAL_POLL_INTERVAL_SHORT, TestConstants.GLOBAL_TIMEOUT, () -> {
            try {
                InstallPlan ip = ResourceManager.getClient().getNonApprovedInstallPlan(namespace, csvName);
                LOGGER.debug("Found InstallPlan {} - {}", ip.getMetadata().getName(), ip.getSpec().getClusterServiceVersionNames());
                return true;
            } catch (NoSuchElementException ex) {
                LOGGER.debug("No new install plan available. Checking again ...");
                return false;
            }
        }, () -> { });
    }

    private KubeUtils() {
    }
}
