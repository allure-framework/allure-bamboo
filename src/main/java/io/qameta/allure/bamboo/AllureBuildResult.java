package io.qameta.allure.bamboo;

import java.io.Serializable;
import java.util.Map;

import static io.qameta.allure.bamboo.AllureConstants.ALLURE_BUILD_REPORT_ARTIFACT_HANDLER;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_BUILD_REPORT_FAILURE_DETAILS;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_BUILD_REPORT_SUCCESS;
import static java.lang.Boolean.parseBoolean;
import static org.sonatype.aether.util.StringUtils.isEmpty;

class AllureBuildResult implements Serializable {
    private final boolean success;
    private String artifactHandlerClass;
    private String failureDetails;

    AllureBuildResult(boolean success) {
        this.success = success;
    }

    static AllureBuildResult fromCustomData(Map<String, String> data) {
        final AllureBuildResult result = new AllureBuildResult(parseBoolean(data.get(ALLURE_BUILD_REPORT_SUCCESS)));
        result.setArtifactHandlerClass(data.get(ALLURE_BUILD_REPORT_ARTIFACT_HANDLER));
        result.setFailureDetails(data.get(ALLURE_BUILD_REPORT_FAILURE_DETAILS));
        return result;
    }

    void toCustomData(Map<String, String> data) {
        data.put(ALLURE_BUILD_REPORT_ARTIFACT_HANDLER, artifactHandlerClass);
        data.put(ALLURE_BUILD_REPORT_SUCCESS, String.valueOf(success));
        data.put(ALLURE_BUILD_REPORT_FAILURE_DETAILS, failureDetails);
    }

    String getArtifactHandlerClass() {
        return artifactHandlerClass;
    }

    void setArtifactHandlerClass(String artifactHandlerClass) {
        this.artifactHandlerClass = artifactHandlerClass;
    }

    boolean isSuccess() {
        return success;
    }

    String getFailureDetails() {
        return failureDetails;
    }

    void setFailureDetails(String failureDetails) {
        this.failureDetails = failureDetails;
    }

    boolean hasInfo(){
        return !isEmpty(this.failureDetails) || !isEmpty(artifactHandlerClass);
    }
}
