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
import io.odh.test.TestConstants;
import io.odh.test.TestUtils;
import lombok.SneakyThrows;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import static io.odh.test.TestUtils.DEFAULT_TIMEOUT_DURATION;
import static io.odh.test.TestUtils.DEFAULT_TIMEOUT_UNIT;

// https://docs.ray.io/en/master/cluster/running-applications/job-submission/api.html
// https://docs.ray.io/en/latest/cluster/running-applications/job-submission/rest.html
public class RayClient {
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION)
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final String baseUrl;

    public RayClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * @return Job ID
     */
    @SneakyThrows
    public String submitJob(String entrypoint) {
        Map<String, Object> body = Map.of("entrypoint", entrypoint,
                "runtime_env", Map.of(),
                "metadata", Map.of("job_submission_id", "123456"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/jobs/"))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .timeout(Duration.of(DEFAULT_TIMEOUT_DURATION, DEFAULT_TIMEOUT_UNIT.toChronoUnit()))
                .build();
        HttpResponse<String> result = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Map data = objectMapper.readValue(result.body(), Map.class);

        return (String) data.get("job_id");
    }

    public void waitForJob(String jobId) {
        HttpRequest request2 = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/jobs/%s".formatted(jobId)))
                .GET()
                .timeout(Duration.of(DEFAULT_TIMEOUT_DURATION, DEFAULT_TIMEOUT_UNIT.toChronoUnit()))
                .build();

        TestUtils.waitFor("ray job to finish executing", TestConstants.GLOBAL_POLL_INTERVAL_SHORT, TestConstants.GLOBAL_TIMEOUT, () -> {
            HttpResponse<String> result;
            try {
                result = httpClient.send(request2, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }

            Map data;
            try {
                data = objectMapper.readValue(result.body(), Map.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            String status = (String) data.get("status");
            return switch (status) {
                case "PENDING", "RUNNING" -> false;
                case "SUCCEEDED" -> true;
                case "STOPPED", "FAILED" -> throw new RuntimeException("Job did not succeed " + result.body());
                default -> throw new RuntimeException("Unexpected status " + status);
            };
        });
    }

    @SneakyThrows
    public String getJobLogs(String jobId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/jobs/%s/logs".formatted(jobId)))
                .GET()
                .timeout(Duration.of(DEFAULT_TIMEOUT_DURATION, DEFAULT_TIMEOUT_UNIT.toChronoUnit()))
                .build();
        HttpResponse<String> result = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Map data = objectMapper.readValue(result.body(), Map.class);
        return (String) data.get("logs");
    }
}
