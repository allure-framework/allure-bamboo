/*
 *  Copyright 2016-2023 Qameta Software OÜ
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
     * The name of artifact.
     */
    static final String ALLURE_ARTIFACT_NAME = "Allure Report";

    /**
     * The executable prefix.
     */
    static final String ALLURE_EXECUTION_PREFIX = "system.builder.allure";

    /**
     * The directory with allure results relative from build directory.
     */
    static final String ALLURE_CONFIG_RESULTS_PATH = "custom.allure.config.results.path";

    /**
     * The subdirectory (or subdirectories) to put generated report into.
     */
    static final String ALLURE_CONFIG_REPORT_PATH = "custom.allure.config.report.path";

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
    static final String ALLURE_CONFIG_STORAGE_TYPE = "custom.allure.config.storage.type";
    static final String ALLURE_CONFIG_DOWNLOAD_ENABLED = "custom.allure.config.download.enabled";
    static final String ALLURE_CONFIG_ENABLED_BY_DEFAULT = "custom.allure.config.enabled.default";
    static final String ALLURE_CONFIG_DOWNLOAD_URL = "custom.allure.config.download.url";
    static final String ALLURE_CONFIG_LOCAL_STORAGE = "custom.allure.config.local.storage";

    private AllureConstants() {
    }
}
