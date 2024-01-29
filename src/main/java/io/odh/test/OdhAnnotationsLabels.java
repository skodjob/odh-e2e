/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test;

public class OdhAnnotationsLabels {
    public static final String OPENSHIFT_DOMAIN = "openshift.io/";
    public static final String ODH_DOMAIN = "opendatahub.io/";

    public static final String LABEL_DASHBOARD = ODH_DOMAIN + "dashboard";
    public static final String LABEL_ODH_MANAGED = ODH_DOMAIN + "odh-managed";
    public static final String LABEL_SIDECAR_ISTIO_INJECT = "sidecar.istio.io/inject";

    public static final String ANNO_SERVICE_MESH = ODH_DOMAIN + "service-mesh";
    public static final String ANNO_NTB_INJECT_OAUTH = "notebooks." + ODH_DOMAIN + "inject-oauth";
    public static final String APP_LABEL_KEY = "app";
    public static final String APP_LABEL_VALUE = "odh-e2e";

}
