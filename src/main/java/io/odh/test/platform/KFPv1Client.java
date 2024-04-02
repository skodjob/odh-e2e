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
import io.odh.test.TestUtils;
import io.odh.test.platform.httpClient.MultipartFormDataBodyPublisher;
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
import java.util.concurrent.atomic.AtomicReference;

import static io.odh.test.TestUtils.DEFAULT_TIMEOUT_DURATION;
import static io.odh.test.TestUtils.DEFAULT_TIMEOUT_UNIT;
import static org.hamcrest.MatcherAssert.assertThat;

public class KFPv1Client {
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION)
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
    public Pipeline importPipeline(String name, String description, String filePath) {
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
    public @Nonnull List<Pipeline> listPipelines() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/apis/v1beta1/pipelines"))
                .GET()
                .timeout(Duration.of(DEFAULT_TIMEOUT_DURATION, DEFAULT_TIMEOUT_UNIT.toChronoUnit()))
                .build();

        HttpResponse<String> reply = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(reply.statusCode(), 200, reply.body());

        PipelineResponse json = objectMapper.readValue(reply.body(), PipelineResponse.class);
        List<Pipeline> pipelines = json.pipelines;

        return pipelines == null ? Collections.emptyList() : pipelines;
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
                return switch (status) {
                    case "Succeeded" -> true;
                    case "Pending", "Running" -> false;
                    case "Skipped", "Failed", "Error" ->
                            throw new AssertionError("Pipeline run failed: " + status + run.get().error);
                    default -> throw new AssertionError("Unexpected pipeline run status: " + status + run.get().error);
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

    public static class PipelineResponse {
        public List<Pipeline> pipelines;
        public int totalSize;
    }

    public static class Pipeline {
        public String id;
        public String name;
    }

    public static class ApiRunDetail {
        public PipelineRun run;
    }

    public static class ApiListRunsResponse {
        public List<PipelineRun> runs;
        public int totalSize;
        public String nextPageToken;
    }

    public static class PipelineRun {
        public String id;
        public String name;
        public PipelineSpec pipelineSpec;

        public String createdAt;
        public String scheduledAt;
        public String finishedAt;
        public String status;
        public String error;
    }

    public static class PipelineSpec {
        public String pipelineId;
        public String pipelineName;

        public String workflowManifest;
    }
}
