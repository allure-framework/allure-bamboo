package io.qameta.allure.bamboo;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_DOWNLOAD_BASE_URL;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_DOWNLOAD_ENABLED;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_ENABLED_BY_DEFAULT;

public class AllureSettingsManager {
    private final PluginSettings settings;

    public AllureSettingsManager(PluginSettingsFactory settingsFactory) {
        this.settings = settingsFactory.createGlobalSettings();
    }

    AllureGlobalConfig getSettings() {
        final String downloadEnabled = (String) settings.get(ALLURE_CONFIG_DOWNLOAD_ENABLED);
        final String enableByDefault = (String) settings.get(ALLURE_CONFIG_ENABLED_BY_DEFAULT);
        final String downloadBaseUrl = (String) settings.get(ALLURE_CONFIG_DOWNLOAD_BASE_URL);
        return new AllureGlobalConfig( downloadEnabled, enableByDefault, downloadBaseUrl);
    }

    void saveSettings(AllureGlobalConfig config) {
        settings.put(ALLURE_CONFIG_DOWNLOAD_ENABLED, String.valueOf(config.isDownloadEnabled()));
        settings.put(ALLURE_CONFIG_ENABLED_BY_DEFAULT, String.valueOf(config.isEnabledByDefault()));
        settings.put(ALLURE_CONFIG_DOWNLOAD_BASE_URL, String.valueOf(config.getDownloadBaseUrl()));
    }
}
