package io.qameta.allure.bamboo;

/**
 * The internal class with some constants needed to Allure plugin.
 */
final class AllureConstants {

    /**
     * The name of artifact.
     */
    static String ALLURE_ARTIFACT_NAME = "Allure Report";
    /**
     * The executable prefix.
     */
    static String ALLURE_EXECUTION_PREFIX = "system.builder.allure";
    /**
     * The directory with allure results relative from build directory.
     */
    static String ALLURE_CONFIG_RESULTS_PATH = "custom.allure.config.results.path";
    /**
     * The subdirectory (or subdirectories) to put generated report into.
     */
    static String ALLURE_CONFIG_REPORT_PATH = "custom.allure.config.report.path";
    /**
     * The name of executable.
     */
    static String ALLURE_CONFIG_EXECUTABLE = "custom.allure.config.executable";

    static String ALLURE_BUILD_REPORT_SUCCESS = "custom.allure.build.report.success";
    static String ALLURE_BUILD_REPORT_ARTIFACT_HANDLER = "custom.allure.build.report.artifact.handler";
    static String ALLURE_BUILD_REPORT_FAILURE_DETAILS = "custom.allure.build.report.output";
    static String ALLURE_CONFIG_ENABLED = "custom.allure.config.enabled";
    static String ALLURE_CONFIG_FAILED_ONLY = "custom.allure.config.failed.only";
    static String ALLURE_CONFIG_ARTIFACT_NAME = "custom.allure.artifact.name";
    static String ALLURE_CONFIG_STORAGE_TYPE = "custom.allure.config.storage.type";
    static String ALLURE_CONFIG_DOWNLOAD_ENABLED = "custom.allure.config.download.enabled";
    static String ALLURE_CONFIG_ENABLED_BY_DEFAULT = "custom.allure.config.enabled.default";
    static String ALLURE_CONFIG_DOWNLOAD_URL = "custom.allure.config.download.url";
    static String ALLURE_CONFIG_DOWNLOAD_CLI_URL = "custom.allure.config.download.cli.url";
    static String ALLURE_CONFIG_LOCAL_STORAGE = "custom.allure.config.local.storage";

    // ALLURE CUSTOM LOGO
    static String ALLURE_CUSTOM_LOGO_ENABLED = "custom.allure.config.logo.enabled";
    static String ALLURE_CUSTOM_LOGO_PATH = "custom.allure.logo.url";

    private AllureConstants() {
    }
}
