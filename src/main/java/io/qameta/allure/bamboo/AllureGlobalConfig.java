package io.qameta.allure.bamboo;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.io.Serializable;
import java.util.Map;

import static io.qameta.allure.bamboo.AllureConstants.*;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Boolean.parseBoolean;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.SystemUtils.getJavaIoTmpDir;
import static org.sonatype.aether.util.StringUtils.isEmpty;

class AllureGlobalConfig implements Serializable {
    private static final String DEFAULT_DOWNLOAD_BASE_URL = "https://github.com/allure-framework/allure2/releases/download/";
    private static final String DEFAULT_CLI_BASE_URL = "https://repo.maven.apache.org/maven2/io/qameta/allure/";
    public static final String DEFAULT_LOCAL_STORAGE_PATH = new File(getJavaIoTmpDir(), "allure-reports").getPath();
    private final boolean downloadEnabled;
    private final boolean customLogoEnabled;
    private final boolean enabledByDefault;
    private final String localStoragePath;
    private final String downloadBaseUrl;
    private final String downloadCliBaseUrl;

    AllureGlobalConfig() {
        this(TRUE.toString(), FALSE.toString(), DEFAULT_DOWNLOAD_BASE_URL, DEFAULT_LOCAL_STORAGE_PATH, DEFAULT_CLI_BASE_URL, TRUE.toString());
    }

    AllureGlobalConfig(String downloadEnabled, String enabledByDefault, String downloadBaseUrl, String localStoragePath, String cmdLineUrl, String customLogoEnable) {
        this.downloadEnabled = isEmpty(downloadEnabled) ? TRUE : parseBoolean(downloadEnabled);
        this.enabledByDefault = isEmpty(enabledByDefault) ? FALSE : parseBoolean(enabledByDefault);
        this.downloadBaseUrl = isEmpty(downloadBaseUrl) ? DEFAULT_DOWNLOAD_BASE_URL : downloadBaseUrl;
        this.downloadCliBaseUrl = isEmpty(cmdLineUrl) ? DEFAULT_CLI_BASE_URL : cmdLineUrl;
        this.localStoragePath = isEmpty(localStoragePath) ? DEFAULT_LOCAL_STORAGE_PATH : localStoragePath;
        this.customLogoEnabled = isEmpty(customLogoEnable) ? TRUE : parseBoolean(customLogoEnable);
    }


    @NotNull
    static AllureGlobalConfig fromContext(Map context) {
        return new AllureGlobalConfig(
                getSingleValue(context, ALLURE_CONFIG_DOWNLOAD_ENABLED, FALSE.toString()),
                getSingleValue(context, ALLURE_CONFIG_ENABLED_BY_DEFAULT, FALSE.toString()),
                getSingleValue(context, ALLURE_CONFIG_DOWNLOAD_URL, DEFAULT_DOWNLOAD_BASE_URL),
                getSingleValue(context, ALLURE_CONFIG_LOCAL_STORAGE, DEFAULT_LOCAL_STORAGE_PATH),
                getSingleValue(context, ALLURE_CONFIG_DOWNLOAD_CLI_URL, DEFAULT_CLI_BASE_URL),
                getSingleValue(context, ALLURE_CUSTOM_LOGO_ENABLED, FALSE.toString())
        );
    }

    @Nullable
    private static String getSingleValue(Map context, String key, String defaultVal) {
        return ofNullable(context.get(key))
                .map(value -> value instanceof String[] ? ((String[]) value)[0] : (String) value)
                .orElse(defaultVal);
    }

    void toContext(@NotNull Map<String, Object> context) {
        context.put(ALLURE_CONFIG_DOWNLOAD_ENABLED, isDownloadEnabled());
        context.put(ALLURE_CONFIG_ENABLED_BY_DEFAULT, isEnabledByDefault());
        context.put(ALLURE_CONFIG_DOWNLOAD_URL, getDownloadBaseUrl());
        context.put(ALLURE_CONFIG_DOWNLOAD_CLI_URL, getDownloadCliBaseUrl());
        context.put(ALLURE_CONFIG_LOCAL_STORAGE, getLocalStoragePath());
        context.put(ALLURE_CUSTOM_LOGO_ENABLED, isCustomLogoEnabled());
    }

    boolean isDownloadEnabled() {
        return downloadEnabled;
    }

    boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    boolean isCustomLogoEnabled() {
        return customLogoEnabled;
    }

    String getDownloadBaseUrl() {
        return downloadBaseUrl;
    }

    String getDownloadCliBaseUrl() {
        return downloadCliBaseUrl;
    }

    public String getLocalStoragePath() {
        return localStoragePath;
    }
}
