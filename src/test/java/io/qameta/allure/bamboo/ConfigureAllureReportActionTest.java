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

import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.junit.MockitoJUnit.rule;

@SuppressWarnings("checkstyle:MultipleStringLiterals")
public class ConfigureAllureReportActionTest {

    @Rule
    public MockitoRule mockitoRule = rule();

    @Mock
    private AllureSettingsManager settingsManager;

    @Test
    public void itShouldLoadExistingSettingsIntoTheFormOnInput() {
        final ConfigureAllureReportAction action = new TestConfigureAllureReportAction(settingsManager);
        final AllureGlobalConfig config = new AllureGlobalConfig("true",
                "true",
                "https://downloads.example/",
                "/var/allure",
                "https://repo.example/",
                "true",
                "true");
        when(settingsManager.getSettings()).thenReturn(config);

        action.prepare();
        action.input();

        assertThat(action.getDownloadBaseUrl(), equalTo("https://downloads.example/"));
        assertThat(action.getLocalStoragePath(), equalTo("/var/allure"));
        assertThat(action.isDownloadEnabled(), equalTo(true));
        assertThat(action.isEnabledByDefault(), equalTo(true));
        assertThat(action.isCustomLogoEnabled(), equalTo(true));
        assertThat(action.isEnabledReportsCleanup(), equalTo(true));
    }

    @Test
    public void itShouldValidateRequiredFields() {
        final ConfigureAllureReportAction action = new TestConfigureAllureReportAction(settingsManager);
        action.setDownloadBaseUrl(" ");
        action.setLocalStoragePath(" ");

        action.validate();

        assertThat(action.getActionErrors(),
                contains("allure.config.download.url.error.required", "allure.config.local.storage.required"));
    }

    @Test
    public void itShouldPersistUpdatedSettings() {
        final ConfigureAllureReportAction action = new TestConfigureAllureReportAction(settingsManager);
        final ArgumentCaptor<AllureGlobalConfig> captor = ArgumentCaptor.forClass(AllureGlobalConfig.class);
        when(settingsManager.getSettings()).thenReturn(new AllureGlobalConfig());
        action.setDownloadEnabled(false);
        action.setEnabledByDefault(true);
        action.setCustomLogoEnabled(true);
        action.setEnabledReportsCleanup(true);
        action.setDownloadBaseUrl("https://downloads.example/");
        action.setLocalStoragePath("/tmp/allure");

        final String result = action.execute();

        verify(settingsManager).saveSettings(captor.capture());
        assertThat(result, equalTo("success"));
        assertFalse(captor.getValue().isDownloadEnabled());
        assertThat(captor.getValue().getDownloadBaseUrl(), equalTo("https://downloads.example/"));
        assertThat(captor.getValue().getLocalStoragePath(), equalTo("/tmp/allure"));
    }

    private static final class TestConfigureAllureReportAction extends ConfigureAllureReportAction {

        private TestConfigureAllureReportAction(final AllureSettingsManager settingsManager) {
            super(settingsManager);
        }

        @Override
        public String getText(final String key) {
            return key;
        }
    }
}
