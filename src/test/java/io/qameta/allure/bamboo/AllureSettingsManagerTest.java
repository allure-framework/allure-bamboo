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
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;

import java.util.HashMap;
import java.util.Map;

import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_CUSTOM_LOGO_ENABLED;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_DOWNLOAD_ENABLED;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_DOWNLOAD_URL;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_ENABLED_BY_DEFAULT;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_LOCAL_STORAGE;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_REPORTS_CLEANUP_ENABLED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.mockito.junit.MockitoJUnit.rule;

@SuppressWarnings("checkstyle:MultipleStringLiterals")
public class AllureSettingsManagerTest {

    @Rule
    public MockitoRule mockitoRule = rule();

    @Mock
    private PluginSettingsFactory settingsFactory;
    @Mock
    private PluginSettings settings;

    @Test
    public void itShouldReadSettingsFromPluginStorage() {
        final Map<String, Object> storage = storage();
        storage.put(ALLURE_CONFIG_DOWNLOAD_ENABLED, "false");
        storage.put(ALLURE_CONFIG_CUSTOM_LOGO_ENABLED, "true");
        storage.put(ALLURE_CONFIG_ENABLED_BY_DEFAULT, "true");
        storage.put(ALLURE_CONFIG_DOWNLOAD_URL, "https://downloads.example/");
        storage.put(ALLURE_CONFIG_LOCAL_STORAGE, "/srv/allure");
        storage.put(ALLURE_CONFIG_REPORTS_CLEANUP_ENABLED, "true");
        final AllureSettingsManager manager = new AllureSettingsManager(settingsFactory);

        final AllureGlobalConfig config = manager.getSettings();

        assertThat(config.isDownloadEnabled(), equalTo(false));
        assertThat(config.isCustomLogoEnabled(), equalTo(true));
        assertThat(config.isEnabledByDefault(), equalTo(true));
        assertThat(config.getDownloadBaseUrl(), equalTo("https://downloads.example/"));
        assertThat(config.getLocalStoragePath(), equalTo("/srv/allure"));
        assertThat(config.isEnabledReportsCleanup(), equalTo(true));
    }

    @Test
    public void itShouldPersistSettingsBackToPluginStorage() {
        final Map<String, Object> storage = storage();
        final AllureSettingsManager manager = new AllureSettingsManager(settingsFactory);
        final AllureGlobalConfig config = new AllureGlobalConfig(false,
                true,
                "https://downloads.example/",
                "/srv/allure",
                true,
                true);

        manager.saveSettings(config);

        assertThat(storage.get(ALLURE_CONFIG_DOWNLOAD_ENABLED), equalTo("false"));
        assertThat(storage.get(ALLURE_CONFIG_CUSTOM_LOGO_ENABLED), equalTo("true"));
        assertThat(storage.get(ALLURE_CONFIG_ENABLED_BY_DEFAULT), equalTo("true"));
        assertThat(storage.get(ALLURE_CONFIG_DOWNLOAD_URL), equalTo("https://downloads.example/"));
        assertThat(storage.get(ALLURE_CONFIG_LOCAL_STORAGE), equalTo("/srv/allure"));
        assertThat(storage.get(ALLURE_CONFIG_REPORTS_CLEANUP_ENABLED), equalTo("true"));
    }

    private Map<String, Object> storage() {
        final Map<String, Object> storage = new HashMap<>();
        when(settingsFactory.createGlobalSettings()).thenReturn(settings);
        when(settings.get(anyString())).thenAnswer(invocation -> storage.get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            storage.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(settings).put(anyString(), any());
        return storage;
    }
}
