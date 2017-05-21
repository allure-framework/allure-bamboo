package io.qameta.allure.bamboo;

import java.io.Serializable;
import java.util.Map;

import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_EXECUTABLE;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_REPORT_DIRECTORY;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_RESULTS_DIRECTORY;

class AllureReportConfig implements Serializable {

    private final String resultsPath;

    private final String reportPath;

    private final String executable;

    private AllureReportConfig(final String resultsPath, String reportPath, String executable) {
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

    static AllureReportConfig fromContext(Map<String, String> context) {
        final String resultsPath = context.get(ALLURE_RESULTS_DIRECTORY);
        final String reportPath = context.get(ALLURE_REPORT_DIRECTORY);
        final String executable = context.get(ALLURE_CONFIG_EXECUTABLE);
        return new AllureReportConfig(resultsPath, reportPath, executable);
    }

}
