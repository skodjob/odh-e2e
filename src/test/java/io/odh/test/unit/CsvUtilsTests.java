/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
// Copyright (c) 2022 yusuke suzuki
package io.odh.test.unit;

import io.odh.test.TestSuite;
import io.odh.test.framework.ExtensionContextParameterResolver;
import io.odh.test.framework.listeners.TestVisualSeparator;
import io.odh.test.utils.CsvUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag(TestSuite.UNIT)
@ExtendWith(ExtensionContextParameterResolver.class)
public class CsvUtilsTests implements TestVisualSeparator {
    @ParameterizedTest(name = "[{index}] Version.fromString({0}) == {1}")
    @CsvSource({
        "2, 2.0.0",
        "2.9, 2.9.0",
        "2.9.0, 2.9.0",
    })
    public void testVersionFromString(String stringVersion, String expected) {
        Assertions.assertEquals(expected, CsvUtils.Version.fromString(stringVersion).toString());
    }

    @ParameterizedTest(name = "[{index}] cmp({0}, {1}) == {2}")
    @CsvSource({
        "2.0.0, 1.0.0, 1",
        "2.0.0, 2.0.0, 0",
        "2.0.0, 3.0.0, -1",
        "0.2.0, 0.1.0, 1",
        "0.2.0, 0.2.0, 0",
        "0.2.0, 0.3.0, -1",
        "0.0.2, 0.0.1, 1",
        "0.0.2, 0.0.2, 0",
        "0.0.2, 0.0.3, -1",
    })
    public void testVersionCompareTo(String v1, String v2, int expected) {
        Assertions.assertEquals(expected, Integer.signum(CsvUtils.Version.fromString(v1).compareTo(CsvUtils.Version.fromString(v2))));
    }
}
