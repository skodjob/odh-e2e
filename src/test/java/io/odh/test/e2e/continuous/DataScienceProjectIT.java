/*
 * Copyright Tealc authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e.continuous;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.odh.test.e2e.Abstract;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.kubeflow.v1.Notebook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("continuous")
public class DataScienceProjectIT extends Abstract {

    private final String DS_PROJECT_NAME = "test";
    private final String DS_WORKBENCH_NAME = "test-workbench";
    MixedOperation<Notebook, KubernetesResourceList<Notebook>, Resource<Notebook>> notebookCli;

    @BeforeAll
    void init() {
        notebookCli = kubeClient.notebookClient();
    }

    @Test
    void checkDataScienceProject() {
        assertTrue(kubeClient.namespaceExists(DS_PROJECT_NAME));

        assertEquals("true",
                kubeClient.getNamespace(DS_PROJECT_NAME).getMetadata().getLabels().getOrDefault("opendatahub.io/dashboard", "false"));

        Notebook testWorkbench = notebookCli.inNamespace(DS_PROJECT_NAME).withName(DS_WORKBENCH_NAME).get();

        assertEquals("true",
                testWorkbench.getMetadata().getLabels().getOrDefault("opendatahub.io/dashboard", "false"));
        assertEquals("true",
                testWorkbench.getMetadata().getLabels().getOrDefault("opendatahub.io/odh-managed", "false"));
    }
}
