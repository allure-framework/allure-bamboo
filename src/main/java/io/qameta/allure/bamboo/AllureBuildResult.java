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

import static io.qameta.allure.bamboo.AllureConstants.ALLURE_BUILD_REPORT_ARTIFACT_HANDLER;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_BUILD_REPORT_FAILURE_DETAILS;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_BUILD_REPORT_SUCCESS;
import static java.lang.Boolean.parseBoolean;
import static org.apache.commons.lang3.StringUtils.isEmpty;

class AllureBuildResult implements Serializable {
    private final boolean success;
    private String artifactHandlerClass;
    private String failureDetails;

    AllureBuildResult(final boolean success) {
        this.success = success;
    }

    AllureBuildResult(final boolean success,
                      final String failureDetails) {
        this.success = success;
        this.failureDetails = failureDetails;
    }

    static AllureBuildResult allureBuildResult(final boolean success,
                                               final String failureDetails) {
        return new AllureBuildResult(success, failureDetails);
    }

    static AllureBuildResult fromCustomData(final Map<String, String> data) {
        final AllureBuildResult result = new AllureBuildResult(parseBoolean(data.get(ALLURE_BUILD_REPORT_SUCCESS)));
        result.setArtifactHandlerClass(data.get(ALLURE_BUILD_REPORT_ARTIFACT_HANDLER));
        result.setFailureDetails(data.get(ALLURE_BUILD_REPORT_FAILURE_DETAILS));
        return result;
    }

    void dumpToCustomData(final Map<String, String> data) {
        data.put(ALLURE_BUILD_REPORT_ARTIFACT_HANDLER, artifactHandlerClass);
        data.put(ALLURE_BUILD_REPORT_SUCCESS, String.valueOf(success));
        data.put(ALLURE_BUILD_REPORT_FAILURE_DETAILS, failureDetails);
    }

    AllureBuildResult withHandlerClass(final String artifactHandlerClass) {
        setArtifactHandlerClass(artifactHandlerClass);
        return this;
    }

    String getArtifactHandlerClass() {
        return artifactHandlerClass;
    }

    void setArtifactHandlerClass(final String artifactHandlerClass) {
        this.artifactHandlerClass = artifactHandlerClass;
    }

    boolean isSuccess() {
        return success;
    }

    String getFailureDetails() {
        return failureDetails;
    }

    void setFailureDetails(final String failureDetails) {
        this.failureDetails = failureDetails;
    }

    boolean hasInfo() {
        return !isEmpty(this.failureDetails) || !isEmpty(artifactHandlerClass);
    }
}
