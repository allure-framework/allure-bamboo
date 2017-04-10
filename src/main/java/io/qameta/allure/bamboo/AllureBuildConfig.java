package io.qameta.allure.bamboo;

import java.io.Serializable;
import java.util.Map;

import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_ENABLED;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_EXECUTABLE;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_FAILED_ONLY;
import static java.lang.Boolean.parseBoolean;
import static org.apache.commons.lang.StringUtils.isEmpty;

public class AllureBuildConfig implements Serializable {
    private final String enabled;
    private final String onlyForFailed;
    private final String executable;

    private AllureBuildConfig(String executable, String enabled, String onlyForFailed) {
        this.executable = executable;
        this.enabled = enabled;
        this.onlyForFailed = onlyForFailed;
    }

    static AllureBuildConfig fromContext(Map context) {
        final String failedOnlyString = (String) context.get(ALLURE_CONFIG_FAILED_ONLY);
        final String enableAllureString = (String) context.get(ALLURE_CONFIG_ENABLED);
        return new AllureBuildConfig(
                (String) context.get(ALLURE_CONFIG_EXECUTABLE),
                enableAllureString,
                failedOnlyString);
    }

    boolean isOnlyForFailed() {
        return parseBoolean(onlyForFailed);
    }

    boolean isEnabledSet() {
        return !isEmpty(enabled);
    }

    public String getExecutable() {
        return executable;
    }

    boolean isEnabled() {
        return !isEnabledSet() || parseBoolean(enabled);
    }
}
