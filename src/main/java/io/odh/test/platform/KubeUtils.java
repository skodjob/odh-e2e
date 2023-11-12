package io.odh.test.platform;


import io.opendatahub.datasciencecluster.v1.datascienceclusterstatus.Conditions;

import java.util.List;

public class KubeUtils {

    public static Conditions getDscConditionByType(List<Conditions> conditions, String type) {
        return conditions.stream().filter(c -> c.getType().equals(type)).findFirst().orElseGet(null);
    }
}
