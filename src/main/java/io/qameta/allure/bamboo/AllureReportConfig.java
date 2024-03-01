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

import java.io.Serializable;
import java.util.Map;

import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_EXECUTABLE;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_REPORT_PATH;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_RESULTS_PATH;

final class AllureReportConfig implements Serializable {

    private final String resultsPath;
    private final String reportPath;
    private final String executable;

    private AllureReportConfig(final String resultsPath,
                               final String reportPath,
                               final String executable) {
        this.resultsPath = resultsPath;
        this.reportPath = reportPath;
        this.executable = executable;
    }

    String getResultsPath() {
        return resultsPath;
    }

    String getReportPath() {
        return reportPath;
    }

    String getExecutable() {
        return executable;
    }

    static AllureReportConfig fromContext(final Map<String, String> context) {
        final String resultsPath = context.get(ALLURE_CONFIG_RESULTS_PATH);
        final String reportPath = context.get(ALLURE_CONFIG_REPORT_PATH);
        final String executable = context.get(ALLURE_CONFIG_EXECUTABLE);
        return new AllureReportConfig(resultsPath, reportPath, executable);
    }

}
