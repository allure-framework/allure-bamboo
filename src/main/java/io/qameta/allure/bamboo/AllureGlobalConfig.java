package io.qameta.allure.bamboo;

import javax.annotation.Nullable;
import java.io.File;
import java.io.Serializable;
import java.util.Map;

import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_DOWNLOAD_ENABLED;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_DOWNLOAD_URL;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_ENABLED_BY_DEFAULT;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_LOCAL_STORAGE;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Boolean.parseBoolean;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.SystemUtils.getJavaIoTmpDir;
import static org.sonatype.aether.util.StringUtils.isEmpty;

class AllureGlobalConfig implements Serializable {
    private static final String DEFAULT_DOWNLOAD_BASE_URL = "https://github.com/allure-framework/allure2/releases/download/";
    public static final String DEFAULT_LOCAL_STORAGE_PATH = new File(getJavaIoTmpDir(), "allure-reports").getPath();
    private final boolean downloadEnabled;
    private final boolean enabledByDefault;
    private final String localStoragePath;
    private final String downloadBaseUrl;

    AllureGlobalConfig() {
        this(TRUE.toString(), FALSE.toString(), DEFAULT_DOWNLOAD_BASE_URL, DEFAULT_LOCAL_STORAGE_PATH);
    }

    AllureGlobalConfig(String downloadEnabled, String enabledByDefault, String downloadBaseUrl, String localStoragePath) {
        this.downloadEnabled = isEmpty(downloadEnabled) ? TRUE : parseBoolean(downloadEnabled);
        this.enabledByDefault = isEmpty(enabledByDefault) ? FALSE : parseBoolean(enabledByDefault);
        this.downloadBaseUrl = isEmpty(downloadBaseUrl) ? DEFAULT_DOWNLOAD_BASE_URL : downloadBaseUrl;
        this.localStoragePath = isEmpty(localStoragePath) ? DEFAULT_LOCAL_STORAGE_PATH : localStoragePath;
    }


    static AllureGlobalConfig fromContext(Map context) {
        return new AllureGlobalConfig(
                getSingleValue(context, ALLURE_CONFIG_DOWNLOAD_ENABLED, TRUE.toString()),
                getSingleValue(context, ALLURE_CONFIG_ENABLED_BY_DEFAULT, FALSE.toString()),
                getSingleValue(context, ALLURE_CONFIG_DOWNLOAD_URL, null),
                getSingleValue(context, ALLURE_CONFIG_LOCAL_STORAGE, null)
        );
    }

    @Nullable
    private static String getSingleValue(Map context, String key, String defaultVal) {
        return ofNullable(context.get(key))
                .map(value -> value instanceof String[] ? ((String[]) value)[0] : (String) value)
                .orElse(defaultVal);
    }

    boolean isDownloadEnabled() {
        return downloadEnabled;
    }

    boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    void toContext(Map<String, Object> context) {
        context.put(ALLURE_CONFIG_DOWNLOAD_ENABLED, isDownloadEnabled());
        context.put(ALLURE_CONFIG_ENABLED_BY_DEFAULT, isEnabledByDefault());
        context.put(ALLURE_CONFIG_DOWNLOAD_URL, getDownloadBaseUrl());
        context.put(ALLURE_CONFIG_LOCAL_STORAGE, getLocalStoragePath());
    }

    String getDownloadBaseUrl() {
        return downloadBaseUrl;
    }

    public String getLocalStoragePath() {
        return localStoragePath;
    }
}
