package io.qameta.allure.bamboo.util;

import org.junit.Test;

import static io.qameta.allure.bamboo.util.ExceptionUtil.stackTraceToString;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class ExceptionUtilTest {

    @Test
    public void itShouldPrintStackTraceIntoString() throws Exception {
        try {
            throw new RuntimeException("Print me please");
        } catch (RuntimeException e) {
            final String trace = stackTraceToString(e);
            assertThat(trace, containsString("RuntimeException: Print me please"));
            assertThat(trace, containsString("itShouldPrintStackTraceIntoString(ExceptionUtilTest.java:14)"));
        }
    }
}