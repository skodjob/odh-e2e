/*
 * Copyright Tealc authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
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
     * Set values
     */
    public static final String RUN_USER = getOrDefault("USER", null);
    public static final String KUBE_USERNAME = getOrDefault(USERNAME_ENV, null);
    public static final String KUBE_PASSWORD = getOrDefault(PASSWORD_ENV, null);
    public static final String KUBE_TOKEN = getOrDefault(TOKEN_ENV, null);
    public static final String KUBE_URL = getOrDefault(URL_ENV, null);

    private Environment() { }

    static {
        String debugFormat = "{}: {}";
        LOGGER.info("Used environment variables:");
        VALUES.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> LOGGER.info(debugFormat, entry.getKey(), entry.getValue()));
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
