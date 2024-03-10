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

public class OdhConstants {
    private OdhConstants() { }

    private static final Logger LOGGER = LoggerFactory.getLogger(OdhConstants.class);
    private static final Map<String, String> VALUES = new HashMap<>();
    // ODH
    private static final String ODH_CONTROLLERS_NAMESPACE = "opendatahub";
    private static final String ODH_DASHBOARD_ROUTE_NAME = "odh-dashboard";
    private static final String ODH_DASHBOARD_CONTROLLER = "odh-dashboard";

    private static final String ODH_BUNDLE_OPERATOR_NAME = "opendatahub-operator-system";
    private static final String ODH_MONITORING_NAMESPACE = "odh-monitoring";


    // ODH OLM
    private static final String ODH_OLM_OPERATOR_NAME = "opendatahub-operator";
    private static final String ODH_OLM_OPERATOR_NAMESPACE = "openshift-operators";
    private static final String ODH_OLM_OPERATOR_DEPLOYMENT_NAME = "opendatahub-operator-controller-manager";
    private static final String ODH_OLM_SOURCE_NAME = "community-operators";
    private static final String ODH_OLM_APP_BUNDLE_PREFIX = "opendatahub-operator";
    private static final String ODH_OLM_OPERATOR_CHANNEL = "fast";
    private static final String ODH_DSCI_NAME = "default";
    // TODO - should be changed after 2.5 release
    private static final String ODH_OLM_OPERATOR_VERSION = "v2.8.0";
    private static final String ODH_OLM_UPGRADE_STARTING_OPERATOR_VERSION = "v2.8.0";

    // RHOAI
    private static final String RHOAI_CONTROLLERS_NAMESPACE = "redhat-ods-applications";
    private static final String RHOAI_DASHBOARD_ROUTE_NAME = "rhods-dashboard";
    private static final String RHOAI_DASHBOARD_CONTROLLER = "rhods-dashboard";
    private static final String RHOAI_NOTEBOOKS_NAMESPACE = "rhods-notebooks";
    private static final String RHOAI_DSCI_NAME = "default-dsci";
    // RHOAI OLM
    private static final String RHOAI_OLM_OPERATOR_NAME = "rhods-operator";
    private static final String RHOAI_OLM_OPERATOR_NAMESPACE = "redhat-ods-operator";
    private static final String RHOAI_OLM_OPERATOR_DEPLOYMENT_NAME = "rhods-operator";
    private static final String RHOAI_OLM_SOURCE_NAME = "redhat-operators";
    private static final String RHOAI_OLM_APP_BUNDLE_PREFIX = "rhods-operator";
    private static final String RHOAI_OLM_OPERATOR_CHANNEL = "fast";
    private static final String RHOAI_OLM_OPERATOR_VERSION = "2.7.0";
    private static final String RHOAI_OLM_UPGRADE_STARTING_OPERATOR_VERSION = "2.6.0";
    private static final String RHOAI_MONITORING_NAMESPACE = "redhat-ods-monitoring";

    // Public part
    public static final String DSC_CREATION_SUCCESSFUL_EVENT_NAME = "DataScienceClusterCreationSuccessful";

    public static final String CODEFLARE_DEPLOYMENT_NAME = "codeflare-operator-manager";
    public static final String DS_PIPELINES_OPERATOR = "data-science-pipelines-operator-controller-manager";
    public static final String ETCD = "etcd";
    public static final String KSERVE_OPERATOR = "kserve-controller-manager";
    public static final String KUBERAY_OPERATOR = "kuberay-operator";
    public static final String MODELMESH_OPERATOR = "modelmesh-controller";
    public static final String NOTEBOOK_OPERATOR = "notebook-controller-deployment";
    public static final String ODH_MODEL_OPERATOR = "odh-model-controller";
    public static final String ODH_NOTEBOOK_OPERATOR = "odh-notebook-controller-manager";
    public static final String KUEUE_OPERATOR = "kueue-controller-manager";

    public static final String CONTROLLERS_NAMESPACE = getOdhOrRhoai("CONTROLLERS_NAMESPACE", ODH_CONTROLLERS_NAMESPACE, RHOAI_CONTROLLERS_NAMESPACE);
    public static final String DASHBOARD_ROUTE_NAME = getOdhOrRhoai("DASHBOARD_ROUTE_NAME", ODH_DASHBOARD_ROUTE_NAME, RHOAI_DASHBOARD_ROUTE_NAME);
    public static final String DASHBOARD_CONTROLLER = getOdhOrRhoai("DASHBOARD_CONTROLLER", ODH_DASHBOARD_CONTROLLER, RHOAI_DASHBOARD_CONTROLLER);
    public static final String NOTEBOOKS_NAMESPACE = getOdhOrRhoai("NOTEBOOKS_NAMESPACE", ODH_CONTROLLERS_NAMESPACE, RHOAI_NOTEBOOKS_NAMESPACE);
    public static final String BUNDLE_OPERATOR_NAMESPACE = getOdhOrRhoai("BUNDLE_OPERATOR_NAMESPACE", ODH_BUNDLE_OPERATOR_NAME, RHOAI_OLM_OPERATOR_NAME);
    public static final String DEFAULT_DSCI_NAME = getOdhOrRhoai("DSCI_NAME", ODH_DSCI_NAME, RHOAI_DSCI_NAME);
    public static final String MONITORING_NAMESPACE = getOdhOrRhoai("MONITORING_NAMESPACE", ODH_MONITORING_NAMESPACE, RHOAI_MONITORING_NAMESPACE);
    // OLM env variables
    public static final String OLM_OPERATOR_NAME = getOdhOrRhoai("OLM_OPERATOR_NAME", ODH_OLM_OPERATOR_NAME, RHOAI_OLM_OPERATOR_NAME);
    public static final String OLM_OPERATOR_NAMESPACE = getOdhOrRhoai("OLM_OPERATOR_NAMESPACE", ODH_OLM_OPERATOR_NAMESPACE, RHOAI_OLM_OPERATOR_NAMESPACE);
    public static final String OLM_OPERATOR_DEPLOYMENT_NAME = getOdhOrRhoai("OLM_OPERATOR_DEPLOYMENT_NAME", ODH_OLM_OPERATOR_DEPLOYMENT_NAME, RHOAI_OLM_OPERATOR_DEPLOYMENT_NAME);
    public static final String OLM_APP_BUNDLE_PREFIX = getOdhOrRhoai("OLM_APP_BUNDLE_PREFIX", ODH_OLM_APP_BUNDLE_PREFIX, RHOAI_OLM_APP_BUNDLE_PREFIX);
    public static final String OLM_OPERATOR_VERSION = getOdhOrRhoai("OLM_OPERATOR_VERSION", ODH_OLM_OPERATOR_VERSION, RHOAI_OLM_OPERATOR_VERSION);
    public static final String OLM_SOURCE_NAME = getOdhOrRhoai("OLM_SOURCE_NAME", ODH_OLM_SOURCE_NAME, RHOAI_OLM_SOURCE_NAME);
    public static final String OLM_OPERATOR_CHANNEL = getOdhOrRhoai("OLM_OPERATOR_CHANNEL", ODH_OLM_OPERATOR_CHANNEL, RHOAI_OLM_OPERATOR_CHANNEL);
    public static final String OLM_UPGRADE_STARTING_OPERATOR_VERSION = getOdhOrRhoai("OLM_UPGRADE_STARTING_OPERATOR_VERSION", ODH_OLM_UPGRADE_STARTING_OPERATOR_VERSION, RHOAI_OLM_UPGRADE_STARTING_OPERATOR_VERSION);

    private static <T> T getOdhOrRhoai(String var, T odhValue, T rhoaiValue) {
        T returnValue = odhValue;
        if (!Objects.equals(Environment.PRODUCT, Environment.PRODUCT_ODH)) {
            returnValue = rhoaiValue;
        }
        VALUES.put(var, String.valueOf(returnValue));
        return returnValue;
    }

    static {
        String debugFormat = "{}: {}";
        LoggerUtils.logSeparator("-", 30);
        LOGGER.info("Used OdhConstants:");
        VALUES.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    if (!Objects.equals(entry.getValue(), "null")) {
                        LOGGER.info(debugFormat, entry.getKey(), entry.getValue());
                    }
                });
        LoggerUtils.logSeparator("-", 30);
    }
}
