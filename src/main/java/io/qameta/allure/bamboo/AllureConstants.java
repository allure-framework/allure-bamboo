package io.qameta.allure.bamboo;

/**
 * The internal class with some constants needed to Allure plugin.
 */
public interface AllureConstants {
    /**
     * The directory with allure results relative from build directory.
     */
    String RESULTS_DIRECTORY = "allureResultDirectory";

    /**
     * The default directory with allure results.
     */
    String RESULTS_DIRECTORY_DEFAULT = "allure-results/";

    /**
     * The subdirectory (or subdirectories) to put generated report into.
     */
    String REPORT_PATH_PREFIX = "allureReportPathPrefix";

    /**
     * The default subdirectory (or subdirectories) to put generated report into.
     */
    String REPORT_PATH_PREFIX_DEFAULT = "allure-report/";

    /**
     * The name of executable.
     */
    String EXECUTABLE_LABEL = "executableLabel";

    /**
     * The name of artifact.
     */
    String ARTIFACT_NAME = "Allure Report";

    String ALLURE_BUILD_REPORT_SUCCESS = "custom.allure.build.report.success";
    String ALLURE_BUILD_REPORT_ARTIFACT_HANDLER = "custom.allure.build.report.artifact.handler";
    String ALLURE_BUILD_REPORT_FAILURE_DETAILS = "custom.allure.build.report.output";
    String ALLURE_CONFIG_ENABLED = "custom.allure.config.enabled";
    String ALLURE_CONFIG_FAILED_ONLY = "custom.allure.config.failed.only";
    String ALLURE_CONFIG_EXECUTABLE = "custom.allure.config.executable";
    String ALLURE_CONFIG_STORAGE_PATH = "custom.allure.config.storage.path";
    String ALLURE_CONFIG_STORAGE_TYPE = "custom.allure.config.storage.type";
    String ALLURE_CONFIG_DOWNLOAD_ENABLED = "custom.allure.config.download.enabled";
    String ALLURE_CONFIG_ENABLED_BY_DEFAULT = "custom.allure.config.enabled.default";
}
