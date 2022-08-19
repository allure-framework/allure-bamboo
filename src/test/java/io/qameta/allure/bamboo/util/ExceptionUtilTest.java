package io.qameta.allure.bamboo.util;

import org.junit.Test;

import static io.qameta.allure.bamboo.util.ExceptionUtil.stackTraceToString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class ExceptionUtilTest {

    @Test
    public void itShouldPrintStackTraceIntoString() {
        try {
            throw new RuntimeException("Print me please");
        } catch (RuntimeException e) {
            final String trace = stackTraceToString(e);
            assertThat(trace, containsString("RuntimeException: Print me please"));
            assertThat(trace, containsString("itShouldPrintStackTraceIntoString(ExceptionUtilTest.java:14)"));
        }
    }
}