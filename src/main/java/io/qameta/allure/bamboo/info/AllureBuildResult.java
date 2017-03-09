package io.qameta.allure.bamboo.info;

import java.io.Serializable;
import java.util.Map;

import static io.qameta.allure.bamboo.AllureConstants.ALLURE_BUILD_REPORT_ARTIFACT_HANDLER;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_BUILD_REPORT_FAILURE_DETAILS;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_BUILD_REPORT_SUCCESS;
import static java.lang.Boolean.parseBoolean;
import static org.sonatype.aether.util.StringUtils.isEmpty;

public class AllureBuildResult implements Serializable {
    private final boolean success;
    private String artifactHandlerClass;
    private String failureDetails;

    public AllureBuildResult(boolean success) {
        this.success = success;
    }

    public static AllureBuildResult fromCustomData(Map<String, String> data) {
        final AllureBuildResult result = new AllureBuildResult(parseBoolean(data.get(ALLURE_BUILD_REPORT_SUCCESS)));
        result.setArtifactHandlerClass(data.get(ALLURE_BUILD_REPORT_ARTIFACT_HANDLER));
        result.setFailureDetails(data.get(ALLURE_BUILD_REPORT_FAILURE_DETAILS));
        return result;
    }

    public void toCustomData(Map<String, String> data) {
        data.put(ALLURE_BUILD_REPORT_ARTIFACT_HANDLER, artifactHandlerClass);
        data.put(ALLURE_BUILD_REPORT_SUCCESS, String.valueOf(success));
        data.put(ALLURE_BUILD_REPORT_FAILURE_DETAILS, failureDetails);
    }

    public String getArtifactHandlerClass() {
        return artifactHandlerClass;
    }

    public void setArtifactHandlerClass(String artifactHandlerClass) {
        this.artifactHandlerClass = artifactHandlerClass;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getFailureDetails() {
        return failureDetails;
    }

    public void setFailureDetails(String failureDetails) {
        this.failureDetails = failureDetails;
    }

    public boolean hasInfo(){
        return !isEmpty(this.failureDetails) || !isEmpty(artifactHandlerClass);
    }
}
