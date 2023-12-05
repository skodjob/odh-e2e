/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;

public class KubeUtils {

    static final Logger LOGGER = LoggerFactory.getLogger(KubeUtils.class);

    public static io.opendatahub.datasciencecluster.v1.datascienceclusterstatus.Conditions getDscConditionByType(List<io.opendatahub.datasciencecluster.v1.datascienceclusterstatus.Conditions> conditions, String type) {
        return conditions.stream().filter(c -> c.getType().equals(type)).findFirst().orElseGet(null);
    }

    public static org.kubeflow.v1.notebookstatus.Conditions getNotebookConditionByType(List<org.kubeflow.v1.notebookstatus.Conditions> conditions, String type) {
        return conditions.stream().filter(c -> c.getType().equals(type)).findFirst().orElseGet(null);
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

    public static CompletableFuture<Void> asyncWaitFor(String description, long pollIntervalMs, long timeoutMs, BooleanSupplier ready) {
        LOGGER.info("Waiting for {}", description);
        long deadline = System.currentTimeMillis() + timeoutMs;
        CompletableFuture<Void> future = new CompletableFuture<>();
        Executor delayed = CompletableFuture.delayedExecutor(pollIntervalMs, TimeUnit.MILLISECONDS, EXECUTOR);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                boolean result;
                try {
                    result = ready.getAsBoolean();
                } catch (Exception e) {
                    future.completeExceptionally(e);
                    return;
                }
                long timeLeft = deadline - System.currentTimeMillis();
                if (!future.isDone()) {
                    if (!result) {
                        if (timeLeft >= 0) {
                            if (LOGGER.isTraceEnabled()) {
                                LOGGER.trace("{} not ready, will try again ({}ms till timeout)", description, timeLeft);
                            }
                            delayed.execute(this);
                        } else {
                            future.completeExceptionally(new TimeoutException(String.format("Waiting for %s timeout %s exceeded", description, timeoutMs)));
                        }
                    } else {
                        future.complete(null);
                    }
                }
            }
        };
        r.run();
        return future;
    }

    private KubeUtils() {
    }
}
