package io.qameta.allure.bamboo.config;

import io.qameta.allure.bamboo.AllureConstants;

import java.io.Serializable;
import java.util.Map;

import static java.lang.Boolean.parseBoolean;
import static org.sonatype.aether.util.StringUtils.isEmpty;

public class AllureGlobalConfig implements Serializable {
    private final boolean downloadEnabled;
    private final boolean enabledByDefault;

    public AllureGlobalConfig() {
        this(true, true);
    }

    public AllureGlobalConfig(boolean downloadEnabled, boolean enabledByDefault) {
        this.downloadEnabled = downloadEnabled;
        this.enabledByDefault = enabledByDefault;
    }


    public static AllureGlobalConfig fromContext(Map context) {
        final String downloadEnabled = ((String[]) context.get(AllureConstants.ALLURE_CONFIG_DOWNLOAD_ENABLED))[0];
        final String enabledByDefault = ((String[]) context.get(AllureConstants.ALLURE_CONFIG_ENABLED_BY_DEFAULT))[0];
        return new AllureGlobalConfig(
                isEmpty(downloadEnabled) || parseBoolean(downloadEnabled),
                isEmpty(enabledByDefault) || parseBoolean(enabledByDefault));
    }

    public boolean isDownloadEnabled() {
        return downloadEnabled;
    }

    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    public void toContext(Map<String, Object> context) {
        context.put(AllureConstants.ALLURE_CONFIG_DOWNLOAD_ENABLED, isDownloadEnabled());
        context.put(AllureConstants.ALLURE_CONFIG_ENABLED_BY_DEFAULT, isEnabledByDefault());
    }


    public enum StorageType {
        LOCAL, HANDLER
    }
}
