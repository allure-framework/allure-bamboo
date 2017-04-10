package io.qameta.allure.bamboo;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import io.qameta.allure.bamboo.config.AllureGlobalConfig;
import org.springframework.beans.factory.InitializingBean;

import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_DOWNLOAD_ENABLED;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_ENABLED_BY_DEFAULT;
import static java.lang.Boolean.parseBoolean;
import static org.apache.commons.lang.StringUtils.isEmpty;

public class AllureSettingsManager implements InitializingBean {
    private final PluginSettings settings;

    public AllureSettingsManager(PluginSettingsFactory settingsFactory) {
        this.settings = settingsFactory.createGlobalSettings();
    }

    public AllureGlobalConfig getSettings() {
        final String downloadEnabled = (String) settings.get(ALLURE_CONFIG_DOWNLOAD_ENABLED);
        final String enableByDefault = (String) settings.get(ALLURE_CONFIG_ENABLED_BY_DEFAULT);
        return new AllureGlobalConfig(
                isEmpty(downloadEnabled) || parseBoolean(downloadEnabled),
                isEmpty(enableByDefault) || parseBoolean(enableByDefault));
    }

    public void saveSettings(AllureGlobalConfig config) {
        settings.put(ALLURE_CONFIG_DOWNLOAD_ENABLED, String.valueOf(config.isDownloadEnabled()));
        settings.put(ALLURE_CONFIG_ENABLED_BY_DEFAULT, String.valueOf(config.isEnabledByDefault()));
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }
}
