/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.odh.test.utils;

import io.fabric8.openshift.api.model.operatorhub.v1alpha1.ClusterServiceVersion;
import io.fabric8.openshift.client.OpenShiftClient;
import io.odh.test.OdhConstants;
import io.odh.test.framework.manager.ResourceManager;
import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;

public class CsvUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(NamespaceUtils.class);

    public static @Nullable String getOperatorVersionFromCsv() {
        String name = OdhConstants.OLM_OPERATOR_NAME;
        String namespace = OdhConstants.OLM_OPERATOR_NAMESPACE;
        OpenShiftClient client = (OpenShiftClient) ResourceManager.getKubeClient().getClient();
        List<ClusterServiceVersion> csvs = client.resources(ClusterServiceVersion.class)
                .inNamespace(namespace)
                .withLabel("operators.coreos.com/" + name + "." + namespace, "")
                .list().getItems();
        LOGGER.debug("Found {} datascience operator CSVs", csvs.size());
        if (csvs.isEmpty()) {
            return "";
        }
        assertThat(csvs, Matchers.hasSize(1));
        return csvs.get(0).getSpec().getVersion();
    }
}
