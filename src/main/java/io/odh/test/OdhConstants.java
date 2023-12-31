/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test;

import java.util.Objects;

public class OdhConstants {
    private OdhConstants() { }

    // ODH
    private static final String ODH_CONTROLLERS_NAMESPACE = "opendatahub";
    private static final String ODH_DASHBOARD_ROUTE_NAME = "odh-dashboard";
    private static final String ODH_DASHBOARD_CONTROLLER = "odh-dashboard";

    private static final String ODH_BUNDLE_OPERATOR_NAME = "opendatahub-operator-system";

    // ODH OLM
    private static final String ODH_OLM_OPERATOR_NAME = "opendatahub-operator";
    private static final String ODH_OLM_OPERATOR_NAMESPACE = "openshift-operators";
    private static final String ODH_OLM_OPERATOR_DEPLOYMENT_NAME = "opendatahub-operator-controller-manager";
    private static final String ODH_OLM_SOURCE_NAME = "community-operators";
    private static final String ODH_OLM_APP_BUNDLE_PREFIX = "opendatahub-operator";
    private static final String ODH_OLM_OPERATOR_CHANNEL = "fast";
    private static final String ODH_OLM_OPERATOR_VERSION = "v2.4.0";

    // RHOAI
    private static final String RHOAI_CONTROLLERS_NAMESPACE = "redhat-ods-applications";
    private static final String RHOAI_DASHBOARD_ROUTE_NAME = "rhods-dashboard";
    private static final String RHOAI_DASHBOARD_CONTROLLER = "rhods-dashboard";
    // RHOAI OLM
    private static final String RHOAI_OLM_OPERATOR_NAME = "rhods-operator";
    private static final String RHOAI_OLM_OPERATOR_NAMESPACE = "redhat-ods-operator";
    private static final String RHOAI_OLM_OPERATOR_DEPLOYMENT_NAME = "rhods-operator";
    private static final String RHOAI_OLM_SOURCE_NAME = "redhat-operators";
    private static final String RHOAI_OLM_APP_BUNDLE_PREFIX = "rhods-operator";
    private static final String RHOAI_OLM_OPERATOR_CHANNEL = "stable";
    private static final String RHOAI_OLM_OPERATOR_VERSION = "2.5.0";

    // Public part
    public static final String CODEFLARE_DEPLOYMENT_NAME = "codeflare-operator-manager";
    public static final String DS_PIPELINES_OPERATOR = "data-science-pipelines-operator-controller-manager";
    public static final String ETCD = "etcd";
    public static final String KSERVE_OPERATOR = "kserve-controller-manager";
    public static final String KUBERAY_OPERATOR = "kuberay-operator";
    public static final String MODELMESH_OPERATOR = "modelmesh-controller";
    public static final String NOTEBOOK_OPERATOR = "notebook-controller-deployment";
    public static final String ODH_MODEL_OPERATOR = "odh-model-controller";
    public static final String ODH_NOTEBOOK_OPERATOR = "odh-notebook-controller-manager";
    public static final String TRUSTY_AI_OPERATOR = "trustyai-service-operator-controller-manager";

    public static final String CONTROLLERS_NAMESPACE = getOdhOrRhoai(ODH_CONTROLLERS_NAMESPACE, RHOAI_CONTROLLERS_NAMESPACE);
    public static final String DASHBOARD_ROUTE_NAME = getOdhOrRhoai(ODH_DASHBOARD_ROUTE_NAME, RHOAI_DASHBOARD_ROUTE_NAME);
    public static final String DASHBOARD_CONTROLLER = getOdhOrRhoai(ODH_DASHBOARD_CONTROLLER, RHOAI_DASHBOARD_CONTROLLER);
    public static final String BUNDLE_OPERATOR_NAMESPACE = getOdhOrRhoai(ODH_BUNDLE_OPERATOR_NAME, RHOAI_OLM_OPERATOR_NAME);
    // OLM env variables
    public static final String OLM_OPERATOR_NAME = getOdhOrRhoai(ODH_OLM_OPERATOR_NAME, RHOAI_OLM_OPERATOR_NAME);
    public static final String OLM_OPERATOR_NAMESPACE = getOdhOrRhoai(ODH_OLM_OPERATOR_NAMESPACE, RHOAI_OLM_OPERATOR_NAMESPACE);
    public static final String OLM_OPERATOR_DEPLOYMENT_NAME = getOdhOrRhoai(ODH_OLM_OPERATOR_DEPLOYMENT_NAME, RHOAI_OLM_OPERATOR_DEPLOYMENT_NAME);
    public static final String OLM_APP_BUNDLE_PREFIX = getOdhOrRhoai(ODH_OLM_APP_BUNDLE_PREFIX, RHOAI_OLM_APP_BUNDLE_PREFIX);
    public static final String OLM_OPERATOR_VERSION = getOdhOrRhoai(ODH_OLM_OPERATOR_VERSION, RHOAI_OLM_OPERATOR_VERSION);
    public static final String OLM_SOURCE_NAME = getOdhOrRhoai(ODH_OLM_SOURCE_NAME, RHOAI_OLM_SOURCE_NAME);
    public static final String OLM_OPERATOR_CHANNEL = getOdhOrRhoai(ODH_OLM_OPERATOR_CHANNEL, RHOAI_OLM_OPERATOR_CHANNEL);

    private static <T> T getOdhOrRhoai(T odhValue, T rhoaiValue) {
        T returnValue = odhValue;
        if (!Objects.equals(Environment.PRODUCT, Environment.PRODUCT_DEFAULT)) {
            returnValue = rhoaiValue;
        }
        return returnValue;
    }
}
