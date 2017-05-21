package io.qameta.allure.bamboo;

/**
 * The internal class with some constants needed to Allure plugin.
 */
final class AllureConstants {
    /**
     * The name of artifact.
     */
    static String ARTIFACT_NAME = "Allure Report";

    /**
     * The directory with allure results relative from build directory.
     */
    static String RESULTS_DIRECTORY = "allureResultDirectory";
    /**
     * The default directory with allure results.
     */
    static String RESULTS_DIRECTORY_DEFAULT = "allure-results/";
    /**
     * The subdirectory (or subdirectories) to put generated report into.
     */
    static String REPORT_DIRECTORY = "allureReportPathPrefix";
    /**
     * The default subdirectory (or subdirectories) to put generated report into.
     */
    static String REPORT_PATH_PREFIX_DEFAULT = "allure-report/";
    /**
     * The name of executable.
     */
    static String EXECUTABLE_LABEL = "executableLabel";
    static String ALLURE_BUILD_REPORT_SUCCESS = "custom.allure.build.report.success";
    static String ALLURE_BUILD_REPORT_ARTIFACT_HANDLER = "custom.allure.build.report.artifact.handler";
    static String ALLURE_BUILD_REPORT_FAILURE_DETAILS = "custom.allure.build.report.output";
    static String ALLURE_CONFIG_ENABLED = "custom.allure.config.enabled";
    static String ALLURE_CONFIG_FAILED_ONLY = "custom.allure.config.failed.only";
    static String ALLURE_CONFIG_EXECUTABLE = "custom.allure.config.executable";
    static String ALLURE_CONFIG_STORAGE_TYPE = "custom.allure.config.storage.type";
    static String ALLURE_CONFIG_DOWNLOAD_ENABLED = "custom.allure.config.download.enabled";
    static String ALLURE_CONFIG_ENABLED_BY_DEFAULT = "custom.allure.config.enabled.default";
    static String ALLURE_CONFIG_DOWNLOAD_BASE_URL = "custom.allure.config.download.url";

    private AllureConstants() {
    }
}
