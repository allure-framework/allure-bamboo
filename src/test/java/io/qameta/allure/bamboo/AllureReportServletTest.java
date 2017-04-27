package io.qameta.allure.bamboo;

import org.junit.Test;

import java.util.regex.Matcher;

import static io.qameta.allure.bamboo.AllureReportServlet.getUrlPattern;
import static org.junit.Assert.assertTrue;

public class AllureReportServletTest {

    @Test
    public void itShouldMatchThePattern() throws Exception {
        final Matcher matcher = getUrlPattern().matcher("/plugins/servlet/allure/report/STPCI-STPITCONFLUENCE60/15/");
        assertTrue(matcher.matches());
    }
}