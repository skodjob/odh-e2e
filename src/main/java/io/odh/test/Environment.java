/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test;

import io.odh.test.install.InstallTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
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

    private static String config;

    private static final Map<String, Object> YAML_DATA = loadConfigurationFile();

    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");

    public static final String USER_PATH = System.getProperty("user.dir");

    private static final String CONFIG_FILE_PATH_ENV = "ENV_FILE";

    private static final String USERNAME_ENV = "KUBE_USERNAME";
    private static final String PASSWORD_ENV = "KUBE_PASSWORD";
    private static final String TOKEN_ENV = "KUBE_TOKEN";
    private static final String URL_ENV = "KUBE_URL";
    private static final String PRODUCT_ENV = "PRODUCT";

    private static final String LOG_DIR_ENV = "LOG_DIR";

    /**
     * Install bundle files
     */
    private static final String INSTALL_FILE_ENV = "INSTALL_FILE";
    private static final String INSTALL_FILE_RELEASED_ENV = "INSTALL_FILE_PREVIOUS";
    private static final String OPERATOR_IMAGE_OVERRIDE_ENV = "OPERATOR_IMAGE_OVERRIDE";

    /**
     * OLM env variables
     */
    private static final String OLM_SOURCE_NAME_ENV = "OLM_SOURCE_NAME";
    private static final String OLM_SOURCE_NAMESPACE_ENV = "OLM_SOURCE_NAMESPACE";
    private static final String OLM_OPERATOR_VERSION_ENV = "OLM_OPERATOR_VERSION";
    private static final String OLM_OPERATOR_CHANNEL_ENV = "OLM_OPERATOR_CHANNEL";
    private static final String OPERATOR_INSTALL_TYPE_ENV = "OPERATOR_INSTALL_TYPE";

    public static final String PRODUCT_DEFAULT = "odh";

    /**
     * Set values
     */
    public static final String PRODUCT = getOrDefault(PRODUCT_ENV, PRODUCT_DEFAULT);
    public static final String RUN_USER = getOrDefault("USER", null);
    public static final String KUBE_USERNAME = getOrDefault(USERNAME_ENV, null);
    public static final String KUBE_PASSWORD = getOrDefault(PASSWORD_ENV, null);
    public static final String KUBE_TOKEN = getOrDefault(TOKEN_ENV, null);
    public static final String KUBE_URL = getOrDefault(URL_ENV, null);

    // YAML Bundle
    public static final String INSTALL_FILE_PATH = getOrDefault(INSTALL_FILE_ENV, TestConstants.LATEST_BUNDLE_DEPLOY_FILE);
    public static final String INSTALL_FILE_PREVIOUS_PATH = getOrDefault(INSTALL_FILE_RELEASED_ENV, TestConstants.RELEASED_BUNDLE_DEPLOY_FILE);
    public static final String OPERATOR_IMAGE_OVERRIDE = getOrDefault(OPERATOR_IMAGE_OVERRIDE_ENV, null);

    // OLM env variables
    public static final String OLM_SOURCE_NAME = getOrDefault(OLM_SOURCE_NAME_ENV, OdhConstants.OLM_SOURCE_NAME);
    public static final String OLM_SOURCE_NAMESPACE = getOrDefault(OLM_SOURCE_NAMESPACE_ENV, "openshift-marketplace");
    public static final String OLM_OPERATOR_CHANNEL = getOrDefault(OLM_OPERATOR_CHANNEL_ENV, OdhConstants.OLM_OPERATOR_CHANNEL);
    public static final String OLM_OPERATOR_VERSION = getOrDefault(OLM_OPERATOR_VERSION_ENV, OdhConstants.OLM_OPERATOR_VERSION);

    public static final String OPERATOR_INSTALL_TYPE = getOrDefault(OPERATOR_INSTALL_TYPE_ENV, InstallTypes.BUNDLE.toString());

    public static final Path LOG_DIR = getOrDefault(LOG_DIR_ENV, Paths::get, Paths.get(USER_PATH, "target", "logs")).resolve("test-run-" + DATE_FORMAT.format(LocalDateTime.now()));

    private Environment() {
    }

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

    public static void print() {
    }

    private static String getOrDefault(String varName, String defaultValue) {
        return getOrDefault(varName, String::toString, defaultValue);
    }

    private static <T> T getOrDefault(String var, Function<String, T> converter, T defaultValue) {
        String value = System.getenv(var) != null ?
                System.getenv(var) :
                (Objects.requireNonNull(YAML_DATA).get(var) != null ?
                        YAML_DATA.get(var).toString() :
                        null);
        T returnValue = defaultValue;
        if (value != null) {
            returnValue = converter.apply(value);
        }
        VALUES.put(var, String.valueOf(returnValue));
        return returnValue;
    }

    private static Map<String, Object> loadConfigurationFile() {
        config = System.getenv().getOrDefault(CONFIG_FILE_PATH_ENV,
                Paths.get(System.getProperty("user.dir"), "config.yaml").toAbsolutePath().toString());
        Yaml yaml = new Yaml();
        try {
            File yamlFile = new File(config).getAbsoluteFile();
            return yaml.load(new FileInputStream(yamlFile));
        } catch (IOException ex) {
            LOGGER.info("Json configuration not provider or not exists");
            return Collections.emptyMap();
        }
    }
}
