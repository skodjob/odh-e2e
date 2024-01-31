/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e.continuous;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.odh.test.TestSuite;
import io.odh.test.e2e.Abstract;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.framework.manager.resources.NotebookResource;
import io.odh.test.platform.KubeUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.kubeflow.v1.Notebook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(TestSuite.CONTINUOUS)
public class DataScienceProjectST extends Abstract {
    static final Logger LOGGER = LoggerFactory.getLogger(DataScienceProjectST.class);

    MixedOperation<Notebook, KubernetesResourceList<Notebook>, Resource<Notebook>> notebookCli;

    private static Stream<Arguments> getDsProjects() {
        return Stream.of(
                Arguments.of("project-sammons"),
                Arguments.of("project-hughie"),
                Arguments.of("project-homelander")
        );
    }

    @BeforeAll
    void init() {
        notebookCli = NotebookResource.notebookClient();
    }

    @ParameterizedTest(name = "checkDataScienceProjects-{0}")
    @MethodSource("getDsProjects")
    void checkDataScienceProjects(String dsProjectName) {
        assertTrue(ResourceManager.getKubeClient().namespaceExists(dsProjectName));

        assertEquals("true",
                ResourceManager.getKubeClient().getNamespace(dsProjectName).getMetadata().getLabels().getOrDefault("opendatahub.io/dashboard", "false"));

        notebookCli.inNamespace(dsProjectName).list().getItems().forEach(notebook -> {
            LOGGER.info("Found notebook {} in datascience project {}", notebook.getMetadata().getName(), dsProjectName);
            assertEquals("true",
                    notebook.getMetadata().getLabels().getOrDefault("opendatahub.io/dashboard", "false"));
            assertEquals("true",
                    notebook.getMetadata().getLabels().getOrDefault("opendatahub.io/odh-managed", "false"));

            assertEquals("True", KubeUtils.getNotebookConditionByType(notebook.getStatus().getConditions(), "ContainersReady").getStatus());
            assertEquals("True", KubeUtils.getNotebookConditionByType(notebook.getStatus().getConditions(), "Ready").getStatus());
        });
    }
}
