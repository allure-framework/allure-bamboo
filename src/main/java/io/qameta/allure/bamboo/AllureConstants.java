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

/**
 * The internal class with some constants needed to Allure plugin.
 */
final class AllureConstants {

    /**
     * The executable prefix.
     */
    static final String ALLURE_EXECUTION_PREFIX = "system.builder.allure";
    /**
     * The name of executable.
     */
    static final String ALLURE_CONFIG_EXECUTABLE = "custom.allure.config.executable";

    static final String ALLURE_BUILD_REPORT_SUCCESS = "custom.allure.build.report.success";
    static final String ALLURE_BUILD_REPORT_ARTIFACT_HANDLER = "custom.allure.build.report.artifact.handler";
    static final String ALLURE_BUILD_REPORT_FAILURE_DETAILS = "custom.allure.build.report.output";
    static final String ALLURE_CONFIG_ENABLED = "custom.allure.config.enabled";
    static final String ALLURE_CONFIG_FAILED_ONLY = "custom.allure.config.failed.only";
    static final String ALLURE_CONFIG_ARTIFACT_NAME = "custom.allure.artifact.name";
    static final String ALLURE_CONFIG_DOWNLOAD_ENABLED = "custom.allure.config.download.enabled";
    static final String ALLURE_CONFIG_ENABLED_BY_DEFAULT = "custom.allure.config.enabled.default";
    static final String ALLURE_CONFIG_DOWNLOAD_URL = "custom.allure.config.download.url";
    static final String ALLURE_CONFIG_DOWNLOAD_CLI_URL = "custom.allure.config.download.cli.url";
    static final String ALLURE_CONFIG_LOCAL_STORAGE = "custom.allure.config.local.storage";

    // ALLURE CUSTOM LOGO
    static final String ALLURE_CONFIG_CUSTOM_LOGO_ENABLED = "custom.allure.config.logo.enabled";
    static final String ALLURE_CONFIG_CUSTOM_LOGO_PATH = "custom.allure.logo.url";

    // ALLURE CLEANUP OLD REPORTS
    static final String ALLURE_CONFIG_REPORTS_CLEANUP_ENABLED = "custom.allure.config.reports.cleanup.enabled";
    static final String ALLURE_CONFIG_MAX_STORED_REPORTS_COUNT = "custom.allure.max.stored.reports.count";

    private AllureConstants() {
        // do not instantiate
    }
}
