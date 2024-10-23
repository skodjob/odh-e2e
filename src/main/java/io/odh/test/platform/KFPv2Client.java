/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.platform;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.odh.test.platform.httpClient.MultipartFormDataBodyPublisher;
import io.skodjob.testframe.wait.Wait;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static io.odh.test.TestUtils.DEFAULT_TIMEOUT_DURATION;
import static io.odh.test.TestUtils.DEFAULT_TIMEOUT_UNIT;
import static org.hamcrest.MatcherAssert.assertThat;

// https://www.kubeflow.org/docs/components/pipelines/v2/reference/api/kubeflow-pipeline-api-spec/
public class KFPv2Client {
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION)
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String oauthToken;

    public KFPv2Client(HttpClient httpClient, String baseUrl, String oauthToken) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
        this.oauthToken = oauthToken;
    }

    @SneakyThrows
    public Pipeline importPipeline(String name, String description, String filePath) {
        MultipartFormDataBodyPublisher requestBody = new MultipartFormDataBodyPublisher()
                .addFile("uploadfile", Path.of(filePath), "application/yaml");

        HttpRequest createPipelineRequest = buildRequest()
                .uri(new URI(baseUrl + "/apis/v2beta1/pipelines/upload?name=%s&description=%s".formatted(name, description)))
                .header("Content-Type", requestBody.contentType())
                .POST(requestBody)
                .build();
        HttpResponse<String> responseCreate = httpClient.send(createPipelineRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(responseCreate.body(), responseCreate.statusCode(), Matchers.is(200));

        return objectMapper.readValue(responseCreate.body(), Pipeline.class);
    }

    @SneakyThrows
    public @Nonnull List<Pipeline> listPipelines() {
        HttpRequest request = buildRequest()
                .uri(URI.create(baseUrl + "/apis/v2beta1/pipelines"))
                .GET()
                .build();

        HttpResponse<String> reply = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(reply.statusCode(), 200, reply.body());

        ListPipelinesResponse json = objectMapper.readValue(reply.body(), ListPipelinesResponse.class);
        List<Pipeline> pipelines = json.pipelines;

        return pipelines == null ? Collections.emptyList() : pipelines;
    }

    @SneakyThrows
    public @Nonnull List<PipelineVersion> listPipelineVersions(String pipelineId) {
        HttpRequest request = buildRequest()
                .uri(URI.create(baseUrl + "/apis/v2beta1/pipelines/" + pipelineId + "/versions"))
                .GET()
                .build();

        HttpResponse<String> reply = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(reply.statusCode(), 200, reply.body());

        ListPipelineVersionsResponse json = objectMapper.readValue(reply.body(), ListPipelineVersionsResponse.class);
        List<PipelineVersion> pipelineVersions = json.pipelineVersions;

        return pipelineVersions == null ? Collections.emptyList() : pipelineVersions;
    }

    @SneakyThrows
    public PipelineRun runPipeline(String pipelineTestRunBasename, String pipelineId, Map<?, ?> parameters, String immediate) {
        Assertions.assertEquals(immediate, "Immediate");

        PipelineRun pipelineRun = new PipelineRun();
        pipelineRun.displayName = pipelineTestRunBasename;
        pipelineRun.pipelineVersionReference = new PipelineVersionReference();
        pipelineRun.pipelineVersionReference.pipelineId = pipelineId;
        if (parameters != null) {
            pipelineRun.runtimeConfig = new RuntimeConfig();
            pipelineRun.runtimeConfig.parameters = parameters;
        }
        HttpRequest request = buildRequest()
                .uri(URI.create(baseUrl + "/apis/v2beta1/runs"))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(pipelineRun)))
                .build();
        HttpResponse<String> reply = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        Assertions.assertEquals(reply.statusCode(), 200, reply.body());
        return objectMapper.readValue(reply.body(), PipelineRun.class);
    }

    @SneakyThrows
    public List<PipelineRun> getPipelineRunStatus() {
        HttpRequest request = buildRequest()
                .uri(URI.create(baseUrl + "/apis/v2beta1/runs"))
                .GET()
                .build();
        HttpResponse<String> reply = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        Assertions.assertEquals(reply.statusCode(), 200, reply.body());
        return objectMapper.readValue(reply.body(), ApiListRunsResponse.class).runs;
    }

    @SneakyThrows
    public PipelineRun waitForPipelineRun(String pipelineRunId) {
        HttpRequest request = buildRequest()
                .uri(URI.create(baseUrl + "/apis/v2beta1/runs/" + pipelineRunId))
                .GET()
                .build();

        AtomicReference<PipelineRun> run = new AtomicReference<>();
        Wait.until("pipelineRun to complete", 5000, 10 * 60 * 1000, () -> {
            try {
                HttpResponse<String> reply = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                Assertions.assertEquals(reply.statusCode(), 200, reply.body());
                run.set(objectMapper.readValue(reply.body(), PipelineRun.class));
                String state = run.get().state;
                if (state == null) {
                    return false; // e.g. pod has not been deployed
                }
                // https://github.com/kubeflow/pipelines/issues/7705
                return switch (state) {
                    case "SUCCEEDED" -> true;
                    case "PENDING", "RUNNING" -> false;
                    case "SKIPPED", "FAILED", "CANCELING", "CANCELED", "PAUSED" ->
                            throw new AssertionError("Pipeline run failed: " + state + run.get().error);
                    default -> throw new AssertionError("Unexpected pipeline run status: " + state + run.get().error);
                };
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
        HttpRequest request = buildRequest()
                .uri(URI.create(baseUrl + "/apis/v2beta1/runs/" + runId))
                .DELETE()
                .build();
        HttpResponse<String> reply = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, reply.statusCode(), reply.body());
    }

    @SneakyThrows
    public void deletePipeline(String pipelineId) {
        HttpRequest request = buildRequest()
                .uri(URI.create(baseUrl + "/apis/v2beta1/pipelines/" + pipelineId))
                .DELETE()
                .build();
        HttpResponse<String> reply = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, reply.statusCode(), reply.body());
    }

    @SneakyThrows
    public void deletePipelineVersion(String pipelineId, String pipelineVersionId) {
        HttpRequest request = buildRequest()
                .uri(URI.create(baseUrl + "/apis/v2beta1/pipelines/" + pipelineId + "/versions/" + pipelineVersionId))
                .DELETE()
                .build();
        HttpResponse<String> reply = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, reply.statusCode(), reply.body());
    }

    /// helpers for reading json responses
    /// there is openapi spec, so this can be generated

    public static class ListPipelinesResponse {
        public List<Pipeline> pipelines;
        public int totalSize;
        public String nextPageToken;
    }

    public static class Pipeline {
        public String pipelineId;
        public String displayName;
    }

    public static class ListPipelineVersionsResponse {
        public List<PipelineVersion> pipelineVersions;
        public int totalSize;
        public String nextPageToken;
    }

    public static class PipelineVersion {
        public String pipelineVersionId;
        public String displayName;
    }

    public static class ApiListRunsResponse {
        public List<PipelineRun> runs;
        public int totalSize;
        public String nextPageToken;
    }

    public static class PipelineRun {
        public String runId;
        public String displayName;
        public String pipelineVersionId;
        public PipelineVersionReference pipelineVersionReference;
        public RuntimeConfig runtimeConfig;

        public String createdAt;
        public String scheduledAt;
        public String finishedAt;
        public RunDetails runDetails;

        public String state; // "PENDING", ...
        public String error;
    }

    public static class RunDetails {
        public String pipelineContextId;
        public String pipelineRunContextId;
        public Object taskDetails;
    }

    public static class PipelineVersionReference {
        public String pipelineId;
        public String pipelineVersionId;
    }

    public static class RuntimeConfig {
        public Object parameters;
        public String pipelineRoot;
    }

    private HttpRequest.Builder buildRequest() {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .timeout(Duration.of(DEFAULT_TIMEOUT_DURATION, DEFAULT_TIMEOUT_UNIT.toChronoUnit()));
        if (oauthToken != null) {
            requestBuilder.header("Authorization", "Bearer " + oauthToken);
        }
        return requestBuilder;
    }
}
