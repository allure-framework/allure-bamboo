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

import java.util.regex.Matcher;

import static io.qameta.allure.bamboo.AllureReportServlet.getUrlPattern;
import static org.junit.Assert.assertTrue;

public class AllureReportServletTest {

    @Test
    public void itShouldMatchThePattern() {
        final Matcher matcher = getUrlPattern().matcher("/plugins/servlet/allure/report/STPCI-STPITCONFLUENCE60/15/");
        assertTrue(matcher.matches());
    }
}
