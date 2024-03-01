/*
 *  Copyright 2016-2024 Qameta Software Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.qameta.allure.bamboo;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class AllureCommandLineSupportTest {

    private static final String OUTPUT = "<junit-plugin>, enabled: true\n"
            + "<behaviors-plugin>, enabled: true\n"
            + "<packages-plugin>, enabled: true\n"
            + "<cucumber-json-plugin>, enabled: true\n"
            + "Found 4 results readers\n"
            + "Found %d results for source 1491867175333-0\n"
            + "## Summary\n"
            + "Found %d test cases (%d failed, %d broken)\n"
            + "Success percentage: %s\n"
            + "Creating index.html...\n"
            + "Couldn't find template in cache for \"index.html.ftl\"(\"en_US\", UTF-8, parsed); "
            + "will try to load it.\n"
            + "TemplateLoader.findTemplateSource(\"index.html.ftl\"): Found";

    private final AllureCommandLineSupport support = new AllureCommandLineSupport();

    @Test
    public void itShouldReturnNotContainingTestcasesResult() throws Exception {
        final AllureGenerateResult result = support.parseGenerateOutput(String.format(OUTPUT, 0, 0, 0, 0, "Unknown"));
        assertThat(result.isContainsTestCases(), equalTo(false));
    }

    @Test
    public void itShouldReturnContainingTestcasesResult() throws Exception {
        final AllureGenerateResult result = support.parseGenerateOutput(String.format(OUTPUT, 1, 5, 2, 1, "80"));
        assertThat(result.isContainsTestCases(), equalTo(true));

    }
}
