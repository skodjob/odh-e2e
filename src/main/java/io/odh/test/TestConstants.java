/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test;

import java.text.SimpleDateFormat;
import java.time.Duration;

public class TestConstants {
    public static final String DEFAULT_NAMESPACE = "default";

    public static final String SUBSCRIPTION = "Subscription";
    public static final String OPERATOR_GROUP = "OperatorGroup";

    public static final String APPROVAL_AUTOMATIC = "Automatic";
    public static final String APPROVAL_MANUAL = "Manual";

    public static final String LATEST_BUNDLE_DEPLOY_FILE = "install-files/latest.yaml";
    public static final String RELEASED_BUNDLE_DEPLOY_FILE = "install-files/released.yaml";

    public static final long GLOBAL_POLL_INTERVAL_LONG = Duration.ofSeconds(15).toMillis();
    public static final long GLOBAL_POLL_INTERVAL_MEDIUM = Duration.ofSeconds(10).toMillis();
    public static final long GLOBAL_POLL_INTERVAL_SHORT = Duration.ofSeconds(5).toMillis();
    public static final long GLOBAL_POLL_INTERVAL_1_SEC = Duration.ofSeconds(1).toMillis();
    public static final long GLOBAL_TIMEOUT = Duration.ofMinutes(10).toMillis();
    public static final long GLOBAL_STABILITY_TIME = Duration.ofMinutes(1).toSeconds();
    public static final String LOG_COLLECT_LABEL = "io.odh-e2e.collect-logs";

    // OLM Constants
    public static final String OPENSHIFT_MARKETPLACE_NS = "openshift-marketplace";
    public static final String OPENSHIFT_OPERATORS_NS = "openshift-operators";
    public static final String REDHAT_CATALOG = "redhat-operators";
    public static final String COMMUNITY_CATALOG = "community-operators";
    public static final String CHANNEL_STABLE = "stable";
    public static final String CHANNEL_LATEST = "latest";

    public static final SimpleDateFormat TIMESTAMP_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");

    private TestConstants() {
    }
}
