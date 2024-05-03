/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.odh.test.utils;

import io.fabric8.openshift.api.model.operatorhub.v1alpha1.ClusterServiceVersion;
import io.fabric8.openshift.client.OpenShiftClient;
import io.odh.test.OdhConstants;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;

public class CsvUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(NamespaceUtils.class);

    public static @Nullable String getOperatorVersionFromCsv() {
        String name = OdhConstants.OLM_OPERATOR_NAME;
        String namespace = OdhConstants.OLM_OPERATOR_NAMESPACE;
        OpenShiftClient client = KubeResourceManager.getKubeClient().getOpenShiftClient();
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

    public static class Version implements Comparable<Version> {
        public final int major;
        public final int minor;
        public final int patch;

        public Version(int major, int minor, int patch) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
        }

        public static Version fromString(String versionString) {
            List<Integer> parts = new ArrayList<>();
            try {
                for (String v : versionString.split("\\.")) {
                    parts.add(Integer.parseInt(v));
                }
                if (parts.size() > 3) {
                    throw makeIllegalArgumentException(versionString, null);
                }
                while (parts.size() < 3) {
                    parts.add(0);
                }
            } catch (NumberFormatException e) {
                throw makeIllegalArgumentException(versionString, e);
            }

            return new Version(parts.get(0), parts.get(1), parts.get(2));
        }

        private static IllegalArgumentException makeIllegalArgumentException(String versionString, Throwable throwable) {
            return new IllegalArgumentException("Invalid version: '%s'".formatted(versionString), throwable);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Version version = (Version) o;
            return major == version.major && minor == version.minor && patch == version.patch;
        }

        @Override
        public int hashCode() {
            return Objects.hash(major, minor, patch);
        }

        @Override
        public int compareTo(Version o) {
            if (major != o.major) {
                return major - o.major;
            }
            if (minor != o.minor) {
                return minor - o.minor;
            }
            return patch - o.patch;
        }

        @Override
        public String toString() {
            return "%d.%d.%d".formatted(major, minor, patch);
        }
    }
}
