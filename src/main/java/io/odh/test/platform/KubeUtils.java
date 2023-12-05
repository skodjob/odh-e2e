/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class KubeUtils {

    static final Logger LOGGER = LoggerFactory.getLogger(KubeUtils.class);

    public static io.opendatahub.datasciencecluster.v1.datascienceclusterstatus.Conditions getDscConditionByType(List<io.opendatahub.datasciencecluster.v1.datascienceclusterstatus.Conditions> conditions, String type) {
        return conditions.stream().filter(c -> c.getType().equals(type)).findFirst().orElseGet(null);
    }

    public static org.kubeflow.v1.notebookstatus.Conditions getNotebookConditionByType(List<org.kubeflow.v1.notebookstatus.Conditions> conditions, String type) {
        return conditions.stream().filter(c -> c.getType().equals(type)).findFirst().orElseGet(null);
    }

    private KubeUtils() {
    }
}
