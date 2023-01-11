package io.qameta.allure.bamboo;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_DOWNLOAD_CLI_URL;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_DOWNLOAD_ENABLED;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_DOWNLOAD_URL;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_ENABLED_BY_DEFAULT;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_LOCAL_STORAGE;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CUSTOM_LOGO_ENABLED;

public class AllureSettingsManager {
    private final PluginSettings settings;

    public AllureSettingsManager(PluginSettingsFactory settingsFactory) {
        this.settings = settingsFactory.createGlobalSettings();
    }

    AllureGlobalConfig getSettings() {
        final String downloadEnabled = (String) settings.get(ALLURE_CONFIG_DOWNLOAD_ENABLED);
        final String customLogoEnabled = (String) settings.get(ALLURE_CUSTOM_LOGO_ENABLED);
        final String enableByDefault = (String) settings.get(ALLURE_CONFIG_ENABLED_BY_DEFAULT);
        final String downloadBaseUrl = (String) settings.get(ALLURE_CONFIG_DOWNLOAD_URL);
        final String downloadCliBaseUrl = (String) settings.get(ALLURE_CONFIG_DOWNLOAD_CLI_URL);
        final String localStorage = (String) settings.get(ALLURE_CONFIG_LOCAL_STORAGE);
        return new AllureGlobalConfig( downloadEnabled, enableByDefault, downloadBaseUrl, localStorage, downloadCliBaseUrl, customLogoEnabled);
    }

    void saveSettings(AllureGlobalConfig config) {
        settings.put(ALLURE_CONFIG_DOWNLOAD_ENABLED, String.valueOf(config.isDownloadEnabled()));
        settings.put(ALLURE_CUSTOM_LOGO_ENABLED, String.valueOf(config.isCustomLogoEnabled()));
        settings.put(ALLURE_CONFIG_DOWNLOAD_URL, String.valueOf(config.getDownloadBaseUrl()));
        settings.put(ALLURE_CONFIG_LOCAL_STORAGE, String.valueOf(config.getLocalStoragePath()));
        settings.put(ALLURE_CONFIG_ENABLED_BY_DEFAULT, String.valueOf(config.isEnabledByDefault()));

    }
}
