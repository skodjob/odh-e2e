/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.framework.matchers;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>A LogHasNoUnexpectedErrors is custom matcher to check log form kubernetes client
 * doesn't have any unexpected errors. </p>
 */
public class LogHasNoUnexpectedErrors extends BaseMatcher<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogHasNoUnexpectedErrors.class);

    @Override
    public boolean matches(Object actualValue) {
        if (!"".equals(actualValue)) {
            if (actualValue.toString().contains("Unhandled Exception")) {
                return false;
            }
            // This pattern is used for split each log ine with stack trace if it's there from some reasons
            // It's match start of the line which contains date in format yyyy-mm-dd hh:mm:ss
            String logLineSplitPattern = "[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}";
            for (String line : ((String) actualValue).split(logLineSplitPattern)) {
                if (line.contains("DEBUG") || line.contains("WARN") || line.contains("INFO")) {
                    continue;
                }
                if (line.startsWith("java.lang.NullPointerException")) {
                    return false;
                }
                if (line.contains("NullPointer")) {
                    return false;
                }
                String lineLowerCase = line.toLowerCase(Locale.ENGLISH);
                if (lineLowerCase.contains("error") || lineLowerCase.contains("exception")) {
                    boolean ignoreListResult = false;
                    for (LogIgnoreList value : LogIgnoreList.values()) {
                        Matcher m = Pattern.compile(value.name).matcher(line);
                        if (m.find()) {
                            ignoreListResult = true;
                            break;
                        }
                    }
                    if (!ignoreListResult) {
                        LOGGER.error(line);
                        return false;
                    }
                }
            }
            return true;
        }
        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("The log should not contain unexpected errors.");
    }

    enum LogIgnoreList {
        // This should be removed when https://issues.redhat.com/browse/RHOAIENG-1742 will be done
        MISSING_SERVICE_MESH("servicemeshcontrolplanes.maistra.io\" not found");

        final String name;

        LogIgnoreList(String name) {
            this.name = name;
        }
    }
}
