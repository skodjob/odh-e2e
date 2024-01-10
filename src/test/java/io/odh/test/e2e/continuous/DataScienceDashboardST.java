/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e.continuous;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import io.odh.test.TestSuite;
import io.odh.test.e2e.Abstract;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.framework.manager.resources.NotebookResource;
import io.odh.test.utils.DeploymentUtils;
import lombok.extern.java.Log;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.kubeflow.v1.Notebook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

// TODO: what's difference between continuous and standard?
@Tag(TestSuite.CONTINUOUS)
//TODO: what is ST? system test?
public class DataScienceDashboardST extends Abstract {
    static final Logger LOGGER = LoggerFactory.getLogger(DataScienceDashboardST.class);

    @Test
    void checkDataScienceProjects() throws IOException, InterruptedException {
        try (OpenShiftClient kubeClient = ResourceManager.getClient().getClient().adapt(OpenShiftClient.class)) {
            Service service = kubeClient.services()
                    .inNamespace("redhat-ods-applications")
                    .withName("rhods-dashboard")
                    .get();

            ServicePort port1 = new ServicePortBuilder()
                    .withName("8443-tcp")
                    .withProtocol("TCP")
                    .withPort(8443)
                    .withTargetPort(new IntOrString(8443))
                    .build();

            ServicePort port2 = new ServicePortBuilder()
                    .withName("8080-tcp")
                    .withProtocol("TCP")
                    .withPort(8080)
                    .withTargetPort(new IntOrString(8080))
                    .build();

            Service updatedService = service.edit()
                    .editSpec()
                    .addToPorts(port1, port2)
                    .endSpec()
                    .build();

            Route route = kubeClient.routes().load(new ByteArrayInputStream("""
                    apiVersion: route.openshift.io/v1
                    kind: Route
                    metadata:
                      name: rhods-dashboard-internal
                      namespace: redhat-ods-applications
                      labels:
                        app: rhods-dashboard
                        app.kubernetes.io/part-of: rhods-dashboard
                    spec:
                      port:
                        targetPort: 8080-tcp
                      to:
                        kind: Service
                        name: rhods-dashboard
                      wildcardPolicy: None
                    """.getBytes(StandardCharsets.UTF_8))).item();
            kubeClient.routes().resource(route).create();

            // do I need to wait here?

            // fetch the route object from the server
            Route r2 = kubeClient.routes().resource(route).get();
            String url = "http://%s/api/status".formatted(r2.getSpec().getHost());

            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .GET()
                    .header("x-forwarded-access-token", "asdfadsfasd")
                    .build();
            LOGGER.debug("Making request {}", request);
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            LOGGER.debug("Got response {}", response);

            Assertions.assertEquals(401, response.statusCode());

            service.edit().editSpec().removeFromPorts(port1, port2).endSpec().build();
            kubeClient.routes().resource(route).delete();
        }
    }
}
