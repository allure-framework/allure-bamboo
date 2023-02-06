package io.qameta.allure.bamboo;

import org.apache.commons.validator.routines.UrlValidator;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Map;

import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_ARTIFACT_NAME;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_ENABLED;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_EXECUTABLE;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_FAILED_ONLY;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CUSTOM_LOGO_PATH;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang.StringUtils.isEmpty;

public class AllureBuildConfig implements Serializable {
    private final boolean onlyForFailed;
    private final String executable;
    private final boolean enabled;
    private final String artifactName;
    private final String logoUrl;
    private final static String DEFAULT_ARTIFACT_NAME = "allure-results";
    public final static String DEFAULT_CUSTOM_LOGO_URL = "https://qameta.io/allure-report/img/reportlogo.svg";

    private AllureBuildConfig(String executable, String enabled, String onlyForFailed, String artifactName, String logoUrl) {
        this.onlyForFailed = isEmpty(onlyForFailed) ? TRUE : Boolean.parseBoolean(onlyForFailed);
        this.enabled = isEmpty(enabled) ? FALSE : Boolean.parseBoolean(enabled);
        this.executable = executable;
        this.artifactName = artifactName;
        // If the URL is not a valid URL it will be omitted
        UrlValidator urlValidator = new UrlValidator();
        this.logoUrl = urlValidator.isValid(logoUrl) ? logoUrl : AllureBuildConfig.DEFAULT_CUSTOM_LOGO_URL;
    }

    static AllureBuildConfig fromContext(Map<String, String> context) {
        return new AllureBuildConfig(
                getSingleValue(context, ALLURE_CONFIG_EXECUTABLE, null),
                getSingleValue(context, ALLURE_CONFIG_ENABLED, FALSE.toString()),
                getSingleValue(context, ALLURE_CONFIG_FAILED_ONLY, FALSE.toString()),
                getSingleValue(context, ALLURE_CONFIG_ARTIFACT_NAME, AllureBuildConfig.DEFAULT_ARTIFACT_NAME),
                getSingleValue(context, ALLURE_CUSTOM_LOGO_PATH, AllureBuildConfig.DEFAULT_CUSTOM_LOGO_URL));
    }

    @Nullable
    private static String getSingleValue(Map context, String key, String defaultVal) {
        return ofNullable(context.get(key))
                .map(value -> value instanceof String[] ? ((String[]) value)[0] : (String) value)
                .orElse(defaultVal);
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

    public String getCustomLogoUrl() {
        return this.logoUrl;
    }

}
