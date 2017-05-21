package io.qameta.allure.bamboo;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class AllureCommandLineSupportTest {

    private AllureCommandLineSupport support = new AllureCommandLineSupport();

    @Test
    public void itShouldReturnNotContainingTestcasesResult() throws Exception {
        final AllureGenerateResult result = support.parseGenerateOutput("" +
                "<junit-plugin>, enabled: true\n" +
                "<behaviors-plugin>, enabled: true\n" +
                "<packages-plugin>, enabled: true\n" +
                "<cucumber-json-plugin>, enabled: true\n" +
                "Found 4 results readers\n" +
                "Found 0 results for source 1491867175333-0\n" +
                "## Summary\n" +
                "Found 0 test cases (0 failed, 0 broken)\n" +
                "Success percentage: Unknown\n" +
                "Creating index.html...\n" +
                "Couldn't find template in cache for \"index.html.ftl\"(\"en_US\", UTF-8, parsed); will try to load it.\n" +
                "TemplateLoader.findTemplateSource(\"index.html.ftl\"): Found");
        assertThat(result.isContainsTestCases(), equalTo(false));
    }

    @Test
    public void itShouldReturnContainingTestcasesResult() throws Exception {
        final AllureGenerateResult result = support.parseGenerateOutput("" +
                "<junit-plugin>, enabled: true\n" +
                "<behaviors-plugin>, enabled: true\n" +
                "<packages-plugin>, enabled: true\n" +
                "<cucumber-json-plugin>, enabled: true\n" +
                "Found 4 results readers\n" +
                "Found 1 results for source 1491867175333-0\n" +
                "## Summary\n" +
                "Found 5 test cases (2 failed, 1 broken)\n" +
                "Success percentage: 80\n" +
                "Creating index.html...\n" +
                "Couldn't find template in cache for \"index.html.ftl\"(\"en_US\", UTF-8, parsed); will try to load it.\n" +
                "TemplateLoader.findTemplateSource(\"index.html.ftl\"): Found");
        assertThat(result.isContainsTestCases(), equalTo(true));

    }
}