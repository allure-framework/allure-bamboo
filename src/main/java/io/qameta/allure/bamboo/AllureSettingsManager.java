/*
 *  Copyright 2016-2024 Qameta Software Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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

    public AllureSettingsManager(final PluginSettingsFactory settingsFactory) {
        this.settings = settingsFactory.createGlobalSettings();
    }

    AllureGlobalConfig getSettings() {
        final String downloadEnabled = (String) settings.get(ALLURE_CONFIG_DOWNLOAD_ENABLED);
        final String customLogoEnabled = (String) settings.get(ALLURE_CUSTOM_LOGO_ENABLED);
        final String enableByDefault = (String) settings.get(ALLURE_CONFIG_ENABLED_BY_DEFAULT);
        final String downloadBaseUrl = (String) settings.get(ALLURE_CONFIG_DOWNLOAD_URL);
        final String downloadCliBaseUrl = (String) settings.get(ALLURE_CONFIG_DOWNLOAD_CLI_URL);
        final String localStorage = (String) settings.get(ALLURE_CONFIG_LOCAL_STORAGE);
        return new AllureGlobalConfig(downloadEnabled, enableByDefault,
                downloadBaseUrl, localStorage, downloadCliBaseUrl, customLogoEnabled);
    }

    void saveSettings(final AllureGlobalConfig config) {
        settings.put(ALLURE_CONFIG_DOWNLOAD_ENABLED, String.valueOf(config.isDownloadEnabled()));
        settings.put(ALLURE_CUSTOM_LOGO_ENABLED, String.valueOf(config.isCustomLogoEnabled()));
        settings.put(ALLURE_CONFIG_DOWNLOAD_URL, String.valueOf(config.getDownloadBaseUrl()));
        settings.put(ALLURE_CONFIG_LOCAL_STORAGE, String.valueOf(config.getLocalStoragePath()));
        settings.put(ALLURE_CONFIG_ENABLED_BY_DEFAULT, String.valueOf(config.isEnabledByDefault()));
    }
}
