/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test;

import java.time.Duration;

public class TestConstants {
    public static final String ODH_NAMESPACE = "opendatahub";
    public static final String DEFAULT_NAMESPACE = "default";

    public static final String SUBSCRIPTION = "Subscription";
    public static final String OPERATOR_GROUP = "OperatorGroup";

    public static final long GLOBAL_POLL_INTERVAL = Duration.ofSeconds(10).toMillis();
    public static final long GLOBAL_POLL_INTERVAL_MEDIUM = Duration.ofSeconds(5).toMillis();
    public static final long GLOBAL_POLL_INTERVAL_SHORT = Duration.ofSeconds(1).toMillis();
    public static final long GLOBAL_TIMEOUT = Duration.ofMinutes(5).toMillis();

    private TestConstants() {
    }
}
