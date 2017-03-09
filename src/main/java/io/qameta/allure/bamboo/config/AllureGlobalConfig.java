package io.qameta.allure.bamboo.config;

import io.qameta.allure.bamboo.AllureConstants;

import java.io.Serializable;
import java.util.Map;

import static java.lang.Boolean.parseBoolean;
import static org.sonatype.aether.util.StringUtils.isEmpty;

public class AllureGlobalConfig implements Serializable {
    public static final String DEFAULT_STORAGE_PATH = "/tmp/allure-reports";
    private final boolean downloadEnabled;
    private final boolean enabledByDefault;
    private final String storagePath;

    public AllureGlobalConfig() {
        this(DEFAULT_STORAGE_PATH, true, true);
    }

    public AllureGlobalConfig(String storagePath, boolean downloadEnabled, boolean enabledByDefault) {
        this.downloadEnabled = downloadEnabled;
        this.storagePath = isEmpty(storagePath) ? DEFAULT_STORAGE_PATH : storagePath;
        this.enabledByDefault = enabledByDefault;
    }


    public static AllureGlobalConfig fromContext(Map context) {
        final String storagePath = ((String[]) context.get(AllureConstants.ALLURE_CONFIG_STORAGE_PATH))[0];
        final String downloadEnabled = ((String[]) context.get(AllureConstants.ALLURE_CONFIG_DOWNLOAD_ENABLED))[0];
        final String enabledByDefault = ((String[]) context.get(AllureConstants.ALLURE_CONFIG_ENABLED_BY_DEFAULT))[0];
        return new AllureGlobalConfig(
                isEmpty(storagePath) ? DEFAULT_STORAGE_PATH : storagePath,
                isEmpty(downloadEnabled) || parseBoolean(downloadEnabled),
                isEmpty(enabledByDefault) || parseBoolean(enabledByDefault));
    }

    public String getStoragePath() {
        return storagePath;
    }

    public boolean isDownloadEnabled() {
        return downloadEnabled;
    }

    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    public void toContext(Map<String, Object> context) {
        context.put(AllureConstants.ALLURE_CONFIG_DOWNLOAD_ENABLED, isDownloadEnabled());
        context.put(AllureConstants.ALLURE_CONFIG_STORAGE_PATH, getStoragePath());
        context.put(AllureConstants.ALLURE_CONFIG_ENABLED_BY_DEFAULT, isEnabledByDefault());
    }


    public enum StorageType {
        LOCAL, HANDLER
    }
}
