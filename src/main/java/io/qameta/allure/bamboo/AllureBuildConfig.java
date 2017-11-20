package io.qameta.allure.bamboo;

import java.io.Serializable;
import java.util.Map;

import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_ARTIFACT_NAME;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_ENABLED;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_EXECUTABLE;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_FAILED_ONLY;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.apache.commons.lang.StringUtils.isEmpty;

public class AllureBuildConfig implements Serializable {
    private final boolean onlyForFailed;
    private final String executable;
    private final boolean enabled;
    private final String artifactName;

    private AllureBuildConfig(String executable, String enabled, String onlyForFailed, String artifactName) {
        this.onlyForFailed = isEmpty(onlyForFailed) ? TRUE : Boolean.parseBoolean(onlyForFailed);
        this.enabled = isEmpty(enabled) ? FALSE : Boolean.parseBoolean(enabled);
        this.executable = executable;
        this.artifactName = artifactName;
    }

    static AllureBuildConfig fromContext(Map<String, String> context) {
        final String failedOnlyString = context.get(ALLURE_CONFIG_FAILED_ONLY);
        final String enableAllureString = context.get(ALLURE_CONFIG_ENABLED);
        return new AllureBuildConfig(
                context.get(ALLURE_CONFIG_EXECUTABLE),
                enableAllureString,
                failedOnlyString,
                context.get(ALLURE_CONFIG_ARTIFACT_NAME));
    }

    boolean isOnlyForFailed() {
        return onlyForFailed;
    }

    boolean isEnabledSet() {
        return enabled;
    }

    public String getExecutable() {
        return executable;
    }

    boolean isEnabled() {
        return enabled;
    }

    public String getArtifactName() {
        return artifactName;
    }
}
