package io.qameta.allure.bamboo;

/**
 * The internal class with some constants needed to Allure plugin.
 */
final class AllureConstants {

    /** The directory with allure results relative from build directory. */
    static final String RESULTS_DIRECTORY = "allure.result.directory";

    /** The default directory with allure results. */
    static final String RESULTS_DIRECTORY_DEFAULT = "allure-results/";

    /** The subdirectory (or subdirectories) to put generated report into. */
    static final String REPORT_PATH_PREFIX = "allure.report.path.prefix";

    /** The default subdirectory (or subdirectories) to put generated report into. */
    static final String REPORT_PATH_PREFIX_DEFAULT = "allure-report/";

    /** The name of executable. */
    static final String EXECUTABLE_LABEL = "executable.label";

    /** The name of artifact. */
    static final String ARTIFACT_NAME = "Allure Report";
}
