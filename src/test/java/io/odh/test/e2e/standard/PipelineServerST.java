/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.e2e.standard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.client.OpenShiftClient;
import io.odh.test.OdhAnnotationsLabels;
import io.odh.test.TestUtils;
import io.odh.test.framework.listeners.ResourceManagerDeleteHandler;
import io.odh.test.framework.manager.ResourceManager;
import io.odh.test.platform.httpClient.MultipartFormDataBodyPublisher;
import io.opendatahub.datasciencepipelinesapplications.v1alpha1.DataSciencePipelinesApplicationBuilder;
import io.opendatahub.datasciencepipelinesapplications.v1alpha1.datasciencepipelinesapplicationspec.ApiServer;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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

import static org.hamcrest.MatcherAssert.assertThat;

/// helpers for reading json responses
/// there should be openapi spec, so this can be generated

class PipelineResponse {
    public List<Pipeline> pipelines;
    public int total_size;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Pipeline {
    public String id;
    public String name;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class ApiRunDetail {
    public PipelineRun run;
}

class ApiListRunsResponse {
    public List<PipelineRun> runs;
    public int total_size;
    public String next_page_token;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class PipelineRun {
    public String id;
    public String name;
    public PipelineSpec pipeline_spec;

    public String created_at;
    public String scheduled_at;
    public String finished_at;
    public String status;
    public String error;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class PipelineSpec {
    public String pipeline_id;
    public String pipeline_name;
}

/**
 * Q1: which http library?
 * fabric8 uses okhttp3
 * there is HttpClient in java 17; does not have multipart form post request helper class?
 * found https://github.com/rest-assured/rest-assured
 * <p>
 * Q2: how to deal with jsons from kf pipeline?
 * fabric8 uses jackson; either use ad hoc, or generate client from openapi/swagger
 * rest-assured can read json fields using string paths (example in its readme)
 *
 * try graalpython and simply use python client from java :P
 * https://kf-pipelines.readthedocs.io/en/latest/source/kfp.client.html
 */

@ExtendWith(ResourceManagerDeleteHandler.class)
public class PipelineServerST extends StandardAbstract {
    private static final Logger logger = LoggerFactory.getLogger(PipelineServerST.class);

    ResourceManager resourceManager = ResourceManager.getInstance();
    KubernetesClient client = ResourceManager.getKubeClient().getClient();

    // https://www.kubeflow.org/docs/components/pipelines/v1/tutorials/api-pipelines/
    // https://www.kubeflow.org/docs/components/pipelines/v1/reference/api/kubeflow-pipeline-api-spec/
    /*
    SVC_PORT=$(kubectl -n kubeflow get svc/ml-pipeline -o json | jq ".spec.ports[0].port")
    kubectl port-forward -n kubeflow svc/ml-pipeline ${SVC_PORT}:8888
     */
    @BeforeEach
    void forwarding() {
        /// expose pipeline rest api (this is not handled with CRDs)
//        var svc = client.services().inNamespace("1afterupgrade").withName("ds-pipeline-pipelines-definition").get();
////        System.out.println(svc);
//        client.services().inNamespace("1afterupgrade").withName("ds-pipeline-pipelines-definition").portForward(8888, 8888);
    }

    @Test
    void pipelinesAPIUploadPipeline() throws Exception {
        /// initialize http client

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();


//
//        /// deploy pipeline

        /// first, upload definition
        MultipartFormDataBodyPublisher requestBody = new MultipartFormDataBodyPublisher()
                .addFile("uploadfile", Path.of("/home/jdanek/repos/odh-e2e/src/test/resources/pipelines/iris_pipeline_compiled.yaml"),
                        "application/yaml");

        HttpRequest createPipelineRequest = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:8888/apis/v1beta1/pipelines/upload?name=someName&description=someDescription"))
                .header("Content-Type", requestBody.contentType())
                .POST(requestBody)
                .build();
        var responseCreate = httpClient.send(createPipelineRequest, HttpResponse.BodyHandlers.ofString());
        System.out.println(responseCreate);
        System.out.println(responseCreate.body());

//        Assertions.assertEquals(okResponse.code(), 200, okResponse.message() + " " + okResponse.body().string());
//        var newPipeline = objectMapper.readValue(okResponse.body().string(), Pipeline.class);
//
//        System.out.println(newPipeline);
    }

    @Test
    void pipelinesAPIListPipelines() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        /// list pipelines
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:8888/apis/v1beta1/pipelines"))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();
        var reply = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(reply.body());
        var json = objectMapper.readValue(reply.body(), PipelineResponse.class);
        List<Pipeline> pipelines = json.pipelines;
        Pipeline foundPipeline = null;
        for (var pipeline : pipelines) {
//            if (newPipeline.id.equals(pipeline.id)) {
            foundPipeline = pipeline;
            break;
//            }
        }
        logger.info("{}, {}", foundPipeline.id, foundPipeline.name);
    }

    @Test
    void pipelinesAPIRunPipeline() throws Exception {
        var foundPipeline = new Pipeline();
        foundPipeline.id = "6a9e7a41-8136-4ba2-8990-5df8d94a1c87";

        ObjectMapper objectMapper = new ObjectMapper();

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        /// run pipeline without arguments

        var pipelineRun = new PipelineRun();
        pipelineRun.name = "myrun";
        pipelineRun.pipeline_spec = new PipelineSpec();
        pipelineRun.pipeline_spec.pipeline_id = foundPipeline.id;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:8888/apis/v1beta1/runs"))
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(pipelineRun)
                ))
                .timeout(Duration.ofSeconds(10))
                .build();
        HttpResponse<String> reply = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logger.info("{}", reply.body());
    }

    @Test
    void vipTest() {
        var svc = client.services().inNamespace("1afterupgrade").withName("ds-pipeline-pipelines-definition");
        svc.portForward(8888, 8888);
        waitForPipelineRun("ebdde210-02f3-4a45-bc46-b3870847d912");
    }

    @SneakyThrows
    Pipeline importPipeline(String name, String description, String project, String filePath) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        MultipartFormDataBodyPublisher requestBody = new MultipartFormDataBodyPublisher()
                .addFile("uploadfile", Path.of(filePath), "application/yaml");

        HttpRequest createPipelineRequest = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:8888/apis/v1beta1/pipelines/upload?name=%s&description=%s".formatted(name, description)))
                .header("Content-Type", requestBody.contentType())
                .POST(requestBody)
                .build();
        var responseCreate = httpClient.send(createPipelineRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(responseCreate.body(), responseCreate.statusCode(), Matchers.is(200));

        return new ObjectMapper().readValue(responseCreate.body(), Pipeline.class);
    }

    /// ODS-2206 - Verify user can create and run a data science pipeline in DS Project
    /// ODS-2226 - Verify user can delete components of data science pipeline from DS Pipelines page
    /// https://issues.redhat.com/browse/RHODS-5133
    @Test
    void pipelinez() {
        OpenShiftClient ocClient = (OpenShiftClient) client;

        String PIPELINE_TEST_NAME = "pipeline-test-name";
        String PIPELINE_TEST_DESC = "pipeline-test-desc";
        String PRJ_TITLE = "pipeline-test";
        String PIPELINE_TEST_FILEPATH = "/home/jdanek/repos/odh-e2e/src/test/resources/pipelines/iris_pipeline_compiled.yaml";
        String PIPELINE_TEST_RUN_BASENAME = "pipeline-test-run-basename";

        final String secretName = "secret-name";

        // install openshift pipelines (if not present)
        // create project

        Namespace ns = new NamespaceBuilder()
            .withNewMetadata()
            .withName(PRJ_TITLE)
            .addToLabels(OdhAnnotationsLabels.LABEL_DASHBOARD, "true")
            .addToAnnotations(OdhAnnotationsLabels.ANNO_SERVICE_MESH, "false")
            .endMetadata()
            .build();
        ResourceManager.getInstance().createResourceWithWait(ns);

        Secret secret = new SecretBuilder()
                .withNewMetadata()
                    .withName(secretName)
                    .addToLabels("opendatahub.io/dashboard", "true")
                    .withNamespace(PRJ_TITLE)
                .endMetadata()
                .addToStringData("AWS_ACCESS_KEY_ID", "KEY007")
                .addToStringData("AWS_S3_BUCKET", "HolyGrail")
                .addToStringData("AWS_SECRET_ACCESS_KEY", "gimmeAccessPlz")
                .withType("Opaque")
                .build();
        ResourceManager.getInstance().createResourceWithoutWait(secret);

        // configure pipeline server (with minio, not AWS bucket)
        var dspa = new DataSciencePipelinesApplicationBuilder()
                .withNewMetadata()
                    .withName("pipelines-definition")
                    .withNamespace(PRJ_TITLE)
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
                            .withBucket("HollyGrail")
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

        // connect to the api server we just created, route not available unless I enable oauth
        var route = ocClient.routes()
                .inNamespace(PRJ_TITLE).withName("ds-pipeline-pipelines-definition");

        var svc = client.services().inNamespace(PRJ_TITLE).withName("ds-pipeline-pipelines-definition");
        var endpoints = client.endpoints().inNamespace(PRJ_TITLE).withName("ds-pipeline-pipelines-definition");
        TestUtils.waitFor("pipelines svc to come up", 1000, 1000 * 60, () -> {
            try {
                var endpointset = endpoints.get();
                if (endpointset == null) {
                    return false;
                }
                var subsets = endpointset.getSubsets();
                if (subsets.isEmpty()) {
                    return false;
                }
                for (var subset : subsets) {
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
        System.out.println(route.get());
        // TODO actually I don't know how to do oauth, so lets forward a port
        svc.portForward(8888, 8888);

//        Import Pipeline    name=${PIPELINE_TEST_NAME}
//    ...    description=${PIPELINE_TEST_DESC}
//    ...    project_title=${PRJ_TITLE}
//    ...    filepath=${PIPELINE_TEST_FILEPATH}
//    ...    press_cancel=${TRUE}
//        Pipeline Should Not Be Listed    pipeline_name=${PIPELINE_TEST_NAME}
//    ...    pipeline_description=${PIPELINE_TEST_DESC}
//        Import Pipeline    name=${PIPELINE_TEST_NAME}
//    ...    description=${PIPELINE_TEST_DESC}
//    ...    project_title=${PRJ_TITLE}
//    ...    filepath=${PIPELINE_TEST_FILEPATH}
//    ...    press_cancel=${FALSE}

        var importedPipeline = importPipeline(PIPELINE_TEST_NAME,PIPELINE_TEST_DESC, PRJ_TITLE, PIPELINE_TEST_FILEPATH);

//        Pipeline Context Menu Should Be Working    pipeline_name=${PIPELINE_TEST_NAME}
//        Pipeline Yaml Should Be Readonly    pipeline_name=${PIPELINE_TEST_NAME}

        // n/a

//        Open Data Science Project Details Page    ${PRJ_TITLE}
//        Pipeline Should Be Listed    pipeline_name=${PIPELINE_TEST_NAME}
//    ...    pipeline_description=${PIPELINE_TEST_DESC}
//        Capture Page Screenshot

        List<Pipeline> pipelines = listPipelines(PRJ_TITLE);
        assertThat(pipelines.stream().map(p->p.name).collect(Collectors.toList()), Matchers.contains(PIPELINE_TEST_NAME));

//        ${workflow_name}=    Create Pipeline Run    name=${PIPELINE_TEST_RUN_BASENAME}
//    ...    pipeline_name=${PIPELINE_TEST_NAME}    from_actions_menu=${FALSE}    run_type=Immediate
//    ...    press_cancel=${TRUE}
//        Open Data Science Project Details Page    ${PRJ_TITLE}
//        ${workflow_name}=    Create Pipeline Run    name=${PIPELINE_TEST_RUN_BASENAME}
//    ...    pipeline_name=${PIPELINE_TEST_NAME}    from_actions_menu=${FALSE}    run_type=Immediate
//        Open Data Science Project Details Page    ${PRJ_TITLE}

        var pipelineRun = runPipeline(PIPELINE_TEST_RUN_BASENAME, importedPipeline.id, "Immediate");

//        Wait Until Pipeline Last Run Is Started    pipeline_name=${PIPELINE_TEST_NAME}
//    ...    timeout=10s

        waitForPipelineRun(pipelineRun.id);

//        Wait Until Pipeline Last Run Is Finished    pipeline_name=${PIPELINE_TEST_NAME}
//        Pipeline Last Run Should Be    pipeline_name=${PIPELINE_TEST_NAME}
//    ...    run_name=${PIPELINE_TEST_RUN_BASENAME}
//        Pipeline Last Run Status Should Be    pipeline_name=${PIPELINE_TEST_NAME}
//    ...    status=Completed

        var status = getPipelineRunStatus();
        assertThat(status.stream().filter((r) -> r.id.equals(pipelineRun.id)).map((r) -> r.status).findFirst().get(), Matchers.is("Succeeded"));

//        Pipeline Run Should Be Listed    name=${PIPELINE_TEST_RUN_BASENAME}
//    ...    pipeline_name=${PIPELINE_TEST_NAME}
//        Verify Pipeline Run Deployment Is Successful    project_title=${PRJ_TITLE}
//    ...    workflow_name=${workflow_name}

//        Delete Pipeline Run    ${PIPELINE_TEST_RUN_BASENAME}    ${PIPELINE_TEST_NAME}
//        Delete Pipeline    ${PIPELINE_TEST_NAME}
//        Delete Pipeline Server    ${PRJ_TITLE}

        // deleting whole project, no need to delete individual things in the pipelines server
//        deletePipelineRun();
//        deletePipeline();
    }

    @SneakyThrows
    private List<PipelineRun> getPipelineRunStatus() {
        ObjectMapper objectMapper = new ObjectMapper();

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8888/apis/v1beta1/runs"))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();
        HttpResponse<String> reply = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        Assertions.assertEquals(reply.statusCode(), 200, reply.body());
        return objectMapper.readValue(reply.body(), ApiListRunsResponse.class).runs;
    }

    private void deletePipeline() {
    }

    private void deletePipelineRun() {

    }

    @SneakyThrows
    private PipelineRun waitForPipelineRun(String pipelineRunId) {
        ObjectMapper objectMapper = new ObjectMapper();

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8888/apis/v1beta1/runs/" + pipelineRunId))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

        AtomicReference<PipelineRun> run = new AtomicReference<>();
        TestUtils.waitFor("pipelineRun to complete", 5000, 10 * 60 * 1000, () -> {
            HttpResponse<String> reply = null;
            try {
                reply = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                Assertions.assertEquals(reply.statusCode(), 200, reply.body());
                run.set(objectMapper.readValue(reply.body(), ApiRunDetail.class).run);
                var status = run.get().status;
                if (status.equals("Failed")) { // todo possible statuses?
                    throw new AssertionError("Pipeline run failed: " + status.toString() + run.get().error);
                }
                // "Running"
                return status.equals("Succeeded");
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
    private PipelineRun runPipeline(String pipelineTestRunBasename, String pipelineId, String immediate) {
        Assertions.assertEquals(immediate, "Immediate");

        ObjectMapper objectMapper = new ObjectMapper();

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        var pipelineRun = new PipelineRun();
        pipelineRun.name = pipelineTestRunBasename;
        pipelineRun.pipeline_spec = new PipelineSpec();
        pipelineRun.pipeline_spec.pipeline_id = pipelineId;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8888/apis/v1beta1/runs"))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(pipelineRun)))
                .timeout(Duration.ofSeconds(10))
                .build();
        HttpResponse<String> reply = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        Assertions.assertEquals(reply.statusCode(), 200, reply.body());
        return objectMapper.readValue(reply.body(), ApiRunDetail.class).run;
    }

    @SneakyThrows
    private List<Pipeline> listPipelines(String prjTitle) {
        ObjectMapper objectMapper = new ObjectMapper();

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8888/apis/v1beta1/pipelines"))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

        var reply = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(reply.statusCode(), 200, reply.body());

        var json = objectMapper.readValue(reply.body(), PipelineResponse.class);
        List<Pipeline> pipelines = json.pipelines;

        return pipelines;
    }
}