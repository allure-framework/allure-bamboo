package io.qameta.allure.bamboo;

import java.io.Serializable;
import java.util.Map;

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

    private AllureBuildConfig(String executable, String enabled, String onlyForFailed) {
        this.onlyForFailed = isEmpty(onlyForFailed) ? TRUE : Boolean.parseBoolean(onlyForFailed);
        this.enabled = isEmpty(enabled) ? FALSE : Boolean.parseBoolean(enabled);
        this.executable = executable;
    }

    static AllureBuildConfig fromContext(Map<String, String> context) {
        final String failedOnlyString = context.get(ALLURE_CONFIG_FAILED_ONLY);
        final String enableAllureString = context.get(ALLURE_CONFIG_ENABLED);
        return new AllureBuildConfig(
                context.get(ALLURE_CONFIG_EXECUTABLE),
                enableAllureString,
                failedOnlyString);
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
}
