package io.odh.test.unit;

import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

@EnableKubernetesMockClient(crud = true)
public class UnitTests {
    private KubernetesClient kubernetesClient;
    private KubernetesMockServer server;

    @Test
    void test() {
        Map<String, String> matchLabel = Collections.singletonMap("foo", "bar");
        kubernetesClient.pods().resource(new PodBuilder().withNewMetadata().withName("test").withNamespace("test").endMetadata().build()).create();

    }
}
