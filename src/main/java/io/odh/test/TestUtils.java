/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.InstallPlan;
import io.opendatahub.datasciencecluster.v1.datascienceclusterstatus.Conditions;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.utils.KubeUtils;
import io.skodjob.testframe.wait.Wait;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static io.odh.test.TestConstants.GLOBAL_POLL_INTERVAL_SHORT;
import static io.odh.test.TestConstants.GLOBAL_TIMEOUT;

@SuppressWarnings({"checkstyle:ClassFanOutComplexity"})
public final class TestUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestUtils.class);

    /**
     * Default timeout for asynchronous tests.
     */
    public static final int DEFAULT_TIMEOUT_DURATION = 30;

    /**
     * Default timeout unit for asynchronous tests.
     */
    public static final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.SECONDS;

    private TestUtils() {
        // All static methods
    }

    /**
     * Polls the given HTTP {@code url} until it gives != 503 status code
     */
    public static void waitForServiceNotUnavailable(String url) {
        final HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        waitForServiceNotUnavailable(httpClient, url);
    }

    public static void waitForServiceNotUnavailable(HttpClient httpClient, String url) {
        Wait.until("service to be not unavailable", GLOBAL_POLL_INTERVAL_SHORT, GLOBAL_TIMEOUT, () -> {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.of(DEFAULT_TIMEOUT_DURATION, DEFAULT_TIMEOUT_UNIT.toChronoUnit()))
                    .build();
            try {
                HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
                return response.statusCode() != 503; // Service Unavailable
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(new ThreadFactory() {
        final ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();

        @Override
        public Thread newThread(Runnable r) {
            Thread result = defaultThreadFactory.newThread(r);
            result.setDaemon(true);
            return result;
        }
    });

    public static InputStream getFileFromResourceAsStream(String fileName) {

        // The class loader that loaded the class
        ClassLoader classLoader = TestUtils.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);

        // the stream holding the file content
        if (inputStream == null) {
            throw new IllegalArgumentException("file not found! " + fileName);
        } else {
            return inputStream;
        }

    }

    public static <T> T configFromYaml(String yamlFile, Class<T> c) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            return mapper.readValue(yamlFile, c);
        } catch (InvalidFormatException e) {
            throw new IllegalArgumentException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Path getLogPath(String folderName, ExtensionContext context) {
        String testMethod = context.getDisplayName();
        String testClassName = context.getTestClass().map(Class::getName).orElse("NOCLASS");
        return getLogPath(folderName, testClassName, testMethod);
    }

    public static Path getLogPath(String folderName, TestInfo info) {
        String testMethod = info.getDisplayName();
        String testClassName = info.getTestClass().map(Class::getName).orElse("NOCLASS");
        return getLogPath(folderName, testClassName, testMethod);
    }

    public static Path getLogPath(String folderName, String testClassName, String testMethod) {
        Path path = Environment.LOG_DIR.resolve(Paths.get(folderName, testClassName));
        if (testMethod != null) {
            path = path.resolve(testMethod.replace("(", "").replace(")", ""));
        }
        return path;
    }

    /**
     * Repeat command n-times
     *
     * @param retry count of remaining retries
     * @param fn request function
     * @return The value from the first successful call to the callable
     */
    public static <T> T runUntilPass(int retry, Callable<T> fn) {
        for (int i = 0; i < retry; i++) {
            try {
                LOGGER.debug("Running command, attempt: {}", i);
                return fn.call();
            } catch (Exception | Error ex) {
                LOGGER.warn("Command failed: {}", ex.getMessage());
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        throw new IllegalStateException(String.format("Command wasn't pass in %s attempts", retry));
    }

    public static io.opendatahub.datasciencecluster.v1.datascienceclusterstatus.Conditions getDscConditionByType(List<Conditions> conditions, String type) {
        return conditions.stream().filter(c -> c.getType().equals(type)).findFirst().orElseGet(null);
    }

    public static org.kubeflow.v1.notebookstatus.Conditions getNotebookConditionByType(List<org.kubeflow.v1.notebookstatus.Conditions> conditions, String type) {
        return conditions.stream().filter(c -> c.getType().equals(type)).findFirst().orElseGet(null);
    }

    public static io.kserve.serving.v1beta1.inferenceservicestatus.Conditions getInferenceServiceConditionByType(List<io.kserve.serving.v1beta1.inferenceservicestatus.Conditions> conditions, String type) {
        return conditions.stream().filter(c -> c.getType().equals(type)).findFirst().orElseGet(null);
    }

    public static void clearOdhRemainingResources() {
        KubeResourceManager.getKubeClient().getClient().apiextensions().v1().customResourceDefinitions().list().getItems()
                .stream().filter(crd -> crd.getMetadata().getName().contains("opendatahub.io")).toList()
                .forEach(crd -> {
                    LOGGER.info("Deleting CRD {}", crd.getMetadata().getName());
                    KubeResourceManager.getKubeClient().getClient().resource(crd).delete();
                });
        KubeResourceManager.getKubeClient().getClient().namespaces().withName("opendatahub").delete();
    }

    /**
     * TODO - this should be removed when https://github.com/opendatahub-io/opendatahub-operator/issues/765 will be resolved
     */
    public static void deleteDefaultDSCI() {
        LOGGER.info("Clearing DSCI ...");
        KubeResourceManager.getKubeCmdClient().exec(false, true, Long.valueOf(GLOBAL_TIMEOUT).intValue(),  "delete", "dsci", "--all");
    }

    public static void waitForInstallPlan(String namespace, String csvName) {
        Wait.until(String.format("Install plan with new version: %s:%s", namespace, csvName),
                GLOBAL_POLL_INTERVAL_SHORT, GLOBAL_TIMEOUT, () -> {
                    try {
                        InstallPlan ip = KubeUtils.getNonApprovedInstallPlan(namespace, csvName);
                        LOGGER.debug("Found InstallPlan {} - {}", ip.getMetadata().getName(), ip.getSpec().getClusterServiceVersionNames());
                        return true;
                    } catch (NoSuchElementException ex) {
                        LOGGER.debug("No new install plan available. Checking again ...");
                        return false;
                    }
                }, () -> { });
    }

    public static void waitForEndpoints(String name, Resource<Endpoints> endpoints) {
        Wait.until("%s service endpoints to come up".formatted(name), GLOBAL_POLL_INTERVAL_SHORT, GLOBAL_TIMEOUT, () -> {
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
