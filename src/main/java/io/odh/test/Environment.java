/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Class which holds environment variables for system tests.
 */
public class Environment {

    private static final Logger LOGGER = LoggerFactory.getLogger(Environment.class);
    private static final Map<String, String> VALUES = new HashMap<>();

    private static final String USERNAME_ENV = "KUBE_USERNAME";
    private static final String PASSWORD_ENV = "KUBE_PASSWORD";
    private static final String TOKEN_ENV = "KUBE_TOKEN";
    private static final String URL_ENV = "KUBE_URL";
    /**
     * OLM env variables
     */
    private static final String OLM_OPERATOR_NAME_ENV = "OLM_OPERATOR_NAME";
    private static final String OLM_OPERATOR_NAMESPACE_ENV = "OLM_OPERATOR_NAMESPACE";
    private static final String OLM_OPERATOR_DEPLOYMENT_NAME_ENV = "OLM_OPERATOR_DEPLOYMENT_NAME";
    private static final String OLM_SOURCE_NAME_ENV = "OLM_SOURCE_NAME";
    private static final String OLM_SOURCE_NAMESPACE_ENV = "OLM_SOURCE_NAMESPACE";
    private static final String OLM_APP_BUNDLE_PREFIX_ENV = "OLM_APP_BUNDLE_PREFIX";
    private static final String OLM_OPERATOR_VERSION_ENV = "OLM_OPERATOR_VERSION";
    private static final String OLM_OPERATOR_CHANNEL_ENV = "OLM_OPERATOR_CHANNEL";

    /**
     * Defaults
     */
    public static final String OLM_OPERATOR_NAME_DEFAULT = "opendatahub-operator";
    public static final String OLM_OPERATOR_NAMESPACE_DEFAULT = "openshift-operators";
    public static final String OLM_OPERATOR_DEPLOYMENT_NAME_DEFAULT = "opendatahub-operator-controller-manager";
    public static final String OLM_SOURCE_NAME_DEFAULT = "community-operators";
    public static final String OLM_APP_BUNDLE_PREFIX_DEFAULT = "opendatahub-operator";
    public static final String OLM_OPERATOR_CHANNEL_DEFAULT = "fast";
    public static final String OLM_OPERATOR_VERSION_DEFAULT = "2.4.0";

    /**
     * Set values
     */
    public static final String RUN_USER = getOrDefault("USER", null);
    public static final String KUBE_USERNAME = getOrDefault(USERNAME_ENV, null);
    public static final String KUBE_PASSWORD = getOrDefault(PASSWORD_ENV, null);
    public static final String KUBE_TOKEN = getOrDefault(TOKEN_ENV, null);
    public static final String KUBE_URL = getOrDefault(URL_ENV, null);

    // OLM env variables
    public static final String OLM_OPERATOR_NAME = getOrDefault(OLM_OPERATOR_NAME_ENV, OLM_OPERATOR_NAME_DEFAULT);
    public static final String OLM_OPERATOR_NAMESPACE = getOrDefault(OLM_OPERATOR_NAMESPACE_ENV, OLM_OPERATOR_NAMESPACE_DEFAULT);
    public static final String OLM_OPERATOR_DEPLOYMENT_NAME = getOrDefault(OLM_OPERATOR_DEPLOYMENT_NAME_ENV, OLM_OPERATOR_DEPLOYMENT_NAME_DEFAULT);
    public static final String OLM_SOURCE_NAME = getOrDefault(OLM_SOURCE_NAME_ENV, OLM_SOURCE_NAME_DEFAULT);
    public static final String OLM_SOURCE_NAMESPACE = getOrDefault(OLM_SOURCE_NAMESPACE_ENV, "openshift-marketplace");
    public static final String OLM_APP_BUNDLE_PREFIX = getOrDefault(OLM_APP_BUNDLE_PREFIX_ENV, OLM_APP_BUNDLE_PREFIX_DEFAULT);
    public static final String OLM_OPERATOR_CHANNEL = getOrDefault(OLM_OPERATOR_CHANNEL_ENV, OLM_OPERATOR_CHANNEL_DEFAULT);
    public static final String OLM_OPERATOR_VERSION = getOrDefault(OLM_OPERATOR_VERSION_ENV, OLM_OPERATOR_VERSION_DEFAULT);
    private Environment() { }

    static {
        String debugFormat = "{}: {}";
        LOGGER.info("Used environment variables:");
        VALUES.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    if (!Objects.equals(entry.getValue(), "null")) {
                        LOGGER.info(debugFormat, entry.getKey(), entry.getValue());
                    }
                });
    }

    public static void print() { }

    private static String getOrDefault(String varName, String defaultValue) {
        return getOrDefault(varName, String::toString, defaultValue);
    }

    private static <T> T getOrDefault(String var, Function<String, T> converter, T defaultValue) {
        String value = System.getenv(var);
        T returnValue = defaultValue;
        if (value != null) {
            returnValue = converter.apply(value);
        }
        VALUES.put(var, String.valueOf(returnValue));
        return returnValue;
    }
}
