/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e.standard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.fabric8.kubernetes.api.model.ContainerStateTerminated;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.LocalPortForward;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import io.odh.test.Environment;
import io.odh.test.OdhAnnotationsLabels;
import io.odh.test.TestConstants;
import io.odh.test.TestUtils;
import io.odh.test.framework.listeners.ResourceManagerDeleteHandler;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.platform.httpClient.MultipartFormDataBodyPublisher;
import io.odh.test.utils.DscUtils;
import io.opendatahub.datasciencecluster.v1.DataScienceCluster;
import io.opendatahub.datasciencepipelinesapplications.v1alpha1.DataSciencePipelinesApplication;
import io.opendatahub.datasciencepipelinesapplications.v1alpha1.DataSciencePipelinesApplicationBuilder;
import io.opendatahub.datasciencepipelinesapplications.v1alpha1.datasciencepipelinesapplicationspec.ApiServer;
import io.opendatahub.dscinitialization.v1.DSCInitialization;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static io.odh.test.TestUtils.DEFAULT_TIMEOUT_DURATION;
import static io.odh.test.TestUtils.DEFAULT_TIMEOUT_UNIT;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Q2: how to deal with jsons from kf pipeline?
 * fabric8 uses jackson; either use ad hoc, or generate client from openapi/swagger
 * rest-assured can read json fields using string paths (example in its readme)
 *
 * try graalpython and simply use python client from java :P
 * https://kf-pipelines.readthedocs.io/en/latest/source/kfp.client.html
 */

@ExtendWith(ResourceManagerDeleteHandler.class)
public class PipelineServerST extends StandardAbstract {
    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineServerST.class);

    private static final String DS_PROJECT_NAME = "test-pipelines";

    private final ResourceManager resourceManager = ResourceManager.getInstance();
    private final KubernetesClient client = ResourceManager.getKubeClient().getClient();

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

        ResourceManager.getInstance().createResourceWithWait(dsci);
        ResourceManager.getInstance().createResourceWithWait(dsc);
    }

    /// ODS-2206 - Verify user can create and run a data science pipeline in DS Project
    /// ODS-2226 - Verify user can delete components of data science pipeline from DS Pipelines page
    /// https://issues.redhat.com/browse/RHODS-5133
    @Test
    void testUserCanCreateRunAndDeleteADSPipelineFromDSProject() throws IOException {
        OpenShiftClient ocClient = (OpenShiftClient) client;

        final String pipelineTestName = "pipeline-test-name";
        final String pipelineTestDesc = "pipeline-test-desc";
        final String prjTitle = "pipeline-test";
        final String pipelineTestFilepath = "src/test/resources/pipelines/iris_pipeline_compiled.yaml";
        final String pipelineWorkflowName = "iris-pipeline";
        final String pipelineTestRunBasename = "pipeline-test-run-basename";

        final String secretName = "secret-name";

        // create project
        Namespace ns = new NamespaceBuilder()
            .withNewMetadata()
            .withName(prjTitle)
            .addToLabels(OdhAnnotationsLabels.LABEL_DASHBOARD, "true")
            .addToAnnotations(OdhAnnotationsLabels.ANNO_SERVICE_MESH, "false")
            .endMetadata()
            .build();
        ResourceManager.getInstance().createResourceWithWait(ns);

        // create minio secret
        Secret secret = new SecretBuilder()
                .withNewMetadata()
                    .withName(secretName)
                    .addToLabels("opendatahub.io/dashboard", "true")
                    .withNamespace(prjTitle)
                .endMetadata()
                .addToStringData("AWS_ACCESS_KEY_ID", "KEY007")
                .addToStringData("AWS_S3_BUCKET", "HolyGrail")
                .addToStringData("AWS_SECRET_ACCESS_KEY", "gimmeAccessPlz")
                .withType("Opaque")
                .build();
        ResourceManager.getInstance().createResourceWithWait(secret);

        // configure pipeline server (with minio, not AWS bucket)
        DataSciencePipelinesApplication dspa = new DataSciencePipelinesApplicationBuilder()
                .withNewMetadata()
                    .withName("pipelines-definition")
                    .withNamespace(prjTitle)
                .endMetadata()
                .withNewSpec()
                    .withNewApiServer()
                        .withApplyTektonCustomResource(true)
                        .withArchiveLogs(false)
                        .withAutoUpdatePipelineDefaultVersion(true)
                        .withCollectMetrics(true)
                        .withDbConfigConMaxLifetimeSec(120L)
                        .withDeploy(true)
                        .withEnableOauth(true)
                        .withEnableSamplePipeline(false)
                        .withInjectDefaultScript(true)
                        .withStripEOF(true)
                        .withTerminateStatus(ApiServer.TerminateStatus.CANCELLED)
                        .withTrackArtifacts(true)
                    .endApiServer()
                    .withNewDatabase()
                        .withDisableHealthCheck(false)
                        .withNewMariaDB()
                            .withDeploy(true)
                            .withPipelineDBName("mlpipeline")
                            .withNewPvcSize("10Gi")
                            .withUsername("mlpipeline")
                        .endMariaDB()
                    .endDatabase()
                    .withNewMlmd()
                        .withDeploy(false)
                    .endMlmd()
                    .withNewObjectStorage()
                        .withDisableHealthCheck(false)
                        // todo: ods-ci uses aws, I think minio is more appropriate here
                        .withNewMinio()
                            .withDeploy(true)
                            .withImage("quay.io/minio/minio")
                            .withNewPvcSize("1Gi")
                            .withBucket("HolyGrail")
                            .withNewS3CredentialsSecret()
                                .withAccessKey("AWS_ACCESS_KEY_ID")
                                .withSecretKey("AWS_SECRET_ACCESS_KEY")
                                .withSecretName(secretName)
                            .endMinioS3CredentialsSecret()
                        .endMinio()
                    .endObjectStorage()
                    .withNewPersistenceAgent()
                        .withDeploy(true)
                        .withNumWorkers(2L)
                    .endPersistenceAgent()
                    .withNewScheduledWorkflow()
                        .withCronScheduleTimezone("UTC")
                        .withDeploy(true)
                    .endScheduledWorkflow()
                .endSpec()
                .build();
        ResourceManager.getInstance().createResourceWithWait(dspa);

        // wait for pipeline api server to come up
        Resource<Endpoints> endpoints = client.endpoints().inNamespace(prjTitle).withName("ds-pipeline-pipelines-definition");
        waitForEndpoints(endpoints);

        // connect to the api server we just created, route not available unless I enable oauth
        Resource<Route> route = ocClient.routes()
                .inNamespace(prjTitle).withName("ds-pipeline-pipelines-definition");

        // TODO(jdanek) I don't know how to do oauth, so lets forward a port
        ServiceResource<Service> svc = client.services().inNamespace(prjTitle).withName("ds-pipeline-pipelines-definition");
        try (LocalPortForward portForward = svc.portForward(8888, 0)) {
            KFPv1Client kfpv1Client = new KFPv1Client("http://localhost:%d".formatted(portForward.getLocalPort()));

            // WORKAROUND(RHOAIENG-3250): delete sample pipeline present on ODH
            if (Environment.PRODUCT.equals(Environment.PRODUCT_DEFAULT)) {
                for (KFPv1Client.Pipeline pipeline : kfpv1Client.listPipelines()) {
                    kfpv1Client.deletePipeline(pipeline.id);
                }
            }

            KFPv1Client.Pipeline importedPipeline = kfpv1Client.importPipeline(pipelineTestName, pipelineTestDesc, prjTitle, pipelineTestFilepath);

            List<KFPv1Client.Pipeline> pipelines = kfpv1Client.listPipelines();
            assertThat(pipelines.stream().map(p -> p.name).collect(Collectors.toList()), Matchers.contains(pipelineTestName));

            KFPv1Client.PipelineRun pipelineRun = kfpv1Client.runPipeline(pipelineTestRunBasename, importedPipeline.id, "Immediate");
            Assertions.assertTrue(pipelineRun.pipelineSpec.workflowManifest.contains(pipelineWorkflowName));

            kfpv1Client.waitForPipelineRun(pipelineRun.id);

            List<KFPv1Client.PipelineRun> statuses = kfpv1Client.getPipelineRunStatus();
            assertThat(statuses.stream()
                    .filter(run -> run.id.equals(pipelineRun.id))
                    .map(run -> run.status)
                    .findFirst().get(), Matchers.is("Succeeded"));

            checkPipelineRunK8sDeployments(prjTitle, pipelineWorkflowName + "-" + pipelineRun.id.substring(0, 5));

            kfpv1Client.deletePipelineRun(pipelineRun.id);
            kfpv1Client.deletePipeline(importedPipeline.id);
        }
    }

    private void checkPipelineRunK8sDeployments(String prjTitle, String workflowName) {
        var tektonTaskPods = List.of(
                client.pods().inNamespace(prjTitle).withLabel("tekton.dev/taskRun=" + workflowName + "-data-prep"),
                client.pods().inNamespace(prjTitle).withLabel("tekton.dev/taskRun=" + workflowName + "-train-model"),
                client.pods().inNamespace(prjTitle).withLabel("tekton.dev/taskRun=" + workflowName + "-evaluate-model"),
                client.pods().inNamespace(prjTitle).withLabel("tekton.dev/taskRun=" + workflowName + "-validate-model")
        );

        for (var pods : tektonTaskPods) {
            var podList = pods.list().getItems();
            Assertions.assertEquals(1, podList.size());
            Assertions.assertEquals("Succeeded", podList.get(0).getStatus().getPhase());

            List<ContainerStatus> containerStatuses = podList.get(0).getStatus().getContainerStatuses();
            Assertions.assertNotEquals(0, containerStatuses.size());
            for (var containerStatus : containerStatuses){
                ContainerStateTerminated terminated = containerStatus.getState().getTerminated();
                Assertions.assertNotNull(terminated);
                Assertions.assertEquals(0, terminated.getExitCode());
                Assertions.assertEquals("Completed", terminated.getReason());
            }
        }
    }

    private static void waitForEndpoints(Resource<Endpoints> endpoints) {
        TestUtils.waitFor("pipelines svc to come up", TestConstants.GLOBAL_POLL_INTERVAL_SHORT, TestConstants.GLOBAL_TIMEOUT, () -> {
            try {
                Endpoints endpointset = endpoints.get();
                if (endpointset == null) {
                    return false;
                }
                List<EndpointSubset> subsets = endpointset.getSubsets();
                if (subsets.isEmpty()) {
                    return false;
                }
                for (EndpointSubset subset : subsets) {
                    return !subset.getAddresses().isEmpty();
                }
            } catch (KubernetesClientException e) {
                if (e.getCode() == 404) {
                    return false;
                }
                throw e;
            }
            return false;
        });
    }
}

class KFPv1Client {
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final String baseUrl;

    public KFPv1Client(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @SneakyThrows
    Pipeline importPipeline(String name, String description, String filePath) {
        MultipartFormDataBodyPublisher requestBody = new MultipartFormDataBodyPublisher()
                .addFile("uploadfile", Path.of(filePath), "application/yaml");

        HttpRequest createPipelineRequest = HttpRequest.newBuilder()
                .uri(new URI(baseUrl + "/apis/v1beta1/pipelines/upload?name=%s&description=%s".formatted(name, description)))
                .header("Content-Type", requestBody.contentType())
                .POST(requestBody)
                .timeout(Duration.of(DEFAULT_TIMEOUT_DURATION, DEFAULT_TIMEOUT_UNIT.toChronoUnit()))
                .build();
        HttpResponse<String> responseCreate = httpClient.send(createPipelineRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(responseCreate.body(), responseCreate.statusCode(), Matchers.is(200));

        return objectMapper.readValue(responseCreate.body(), Pipeline.class);
    }

    @SneakyThrows
    public List<Pipeline> listPipelines() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/apis/v1beta1/pipelines"))
                .GET()
                .timeout(Duration.of(DEFAULT_TIMEOUT_DURATION, DEFAULT_TIMEOUT_UNIT.toChronoUnit()))
                .build();

        HttpResponse<String> reply = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(reply.statusCode(), 200, reply.body());

        PipelineResponse json = objectMapper.readValue(reply.body(), PipelineResponse.class);
        List<Pipeline> pipelines = json.pipelines;

        return pipelines;
    }

    @SneakyThrows
    public PipelineRun runPipeline(String pipelineTestRunBasename, String pipelineId, String immediate) {
        Assertions.assertEquals(immediate, "Immediate");

        PipelineRun pipelineRun = new PipelineRun();
        pipelineRun.name = pipelineTestRunBasename;
        pipelineRun.pipelineSpec = new PipelineSpec();
        pipelineRun.pipelineSpec.pipelineId = pipelineId;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/apis/v1beta1/runs"))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(pipelineRun)))
                .timeout(Duration.of(DEFAULT_TIMEOUT_DURATION, DEFAULT_TIMEOUT_UNIT.toChronoUnit()))
                .build();
        HttpResponse<String> reply = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        Assertions.assertEquals(reply.statusCode(), 200, reply.body());
        return objectMapper.readValue(reply.body(), ApiRunDetail.class).run;
    }

    @SneakyThrows
    public List<PipelineRun> getPipelineRunStatus() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/apis/v1beta1/runs"))
                .GET()
                .timeout(Duration.of(DEFAULT_TIMEOUT_DURATION, DEFAULT_TIMEOUT_UNIT.toChronoUnit()))
                .build();
        HttpResponse<String> reply = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        Assertions.assertEquals(reply.statusCode(), 200, reply.body());
        return objectMapper.readValue(reply.body(), ApiListRunsResponse.class).runs;
    }

    @SneakyThrows
    public PipelineRun waitForPipelineRun(String pipelineRunId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/apis/v1beta1/runs/" + pipelineRunId))
                .GET()
                .timeout(Duration.of(DEFAULT_TIMEOUT_DURATION, DEFAULT_TIMEOUT_UNIT.toChronoUnit()))
                .build();

        AtomicReference<PipelineRun> run = new AtomicReference<>();
        TestUtils.waitFor("pipelineRun to complete", 5000, 10 * 60 * 1000, () -> {
            HttpResponse<String> reply = null;
            try {
                reply = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                Assertions.assertEquals(reply.statusCode(), 200, reply.body());
                run.set(objectMapper.readValue(reply.body(), ApiRunDetail.class).run);
                String status = run.get().status;
                if (status == null) {
                    return false; // e.g. pod has not been deployed
                }
                // https://github.com/kubeflow/pipelines/issues/7705
                switch(status) {
                    case "Succeeded":
                        return true;
                    case "Pending":
                    case "Running":
                        return false;
                    case "Skipped":
                    case "Failed":
                    case "Error":
                        throw new AssertionError("Pipeline run failed: " + status + run.get().error);
                    default:
                        throw new AssertionError("Unexpected pipeline run status: " + status + run.get().error);
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        return run.get();
    }

    @SneakyThrows
    public void deletePipelineRun(String runId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/apis/v1beta1/runs/" + runId))
                .DELETE()
                .timeout(Duration.of(DEFAULT_TIMEOUT_DURATION, DEFAULT_TIMEOUT_UNIT.toChronoUnit()))
                .build();
        HttpResponse<String> reply = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, reply.statusCode(), reply.body());
    }

    @SneakyThrows
    public void deletePipeline(String pipelineId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/apis/v1beta1/pipelines/" + pipelineId))
                .DELETE()
                .timeout(Duration.of(DEFAULT_TIMEOUT_DURATION, DEFAULT_TIMEOUT_UNIT.toChronoUnit()))
                .build();
        HttpResponse<String> reply = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, reply.statusCode(), reply.body());
    }

    /// helpers for reading json responses
    /// there is openapi spec, so this can be generated

    static class PipelineResponse {
        public List<Pipeline> pipelines;
        public int totalSize;
    }

    static class Pipeline {
        public String id;
        public String name;
    }

    static class ApiRunDetail {
        public PipelineRun run;
    }

    static class ApiListRunsResponse {
        public List<PipelineRun> runs;
        public int totalSize;
        public String nextPageToken;
    }

    static class PipelineRun {
        public String id;
        public String name;
        public PipelineSpec pipelineSpec;

        public String createdAt;
        public String scheduledAt;
        public String finishedAt;
        public String status;
        public String error;
    }

    static class PipelineSpec {
        public String pipelineId;
        public String pipelineName;

        public String workflowManifest;
    }
}
