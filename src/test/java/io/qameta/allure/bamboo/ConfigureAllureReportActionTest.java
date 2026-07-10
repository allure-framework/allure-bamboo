/*
 *  Copyright 2016-2026 Qameta Software Inc
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

import org.apache.struts2.interceptor.parameter.StrutsParameter;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
        final AllureGlobalConfig config = new AllureGlobalConfig(
                "true",
                "true",
                "https://downloads.example/",
                "/var/allure",
                "true",
                "true"
        );
        when(settingsManager.getSettings()).thenReturn(config);

        action.prepare();
        action.input();

        assertThat(action.getDownloadBaseUrl()).isEqualTo("https://downloads.example/");
        assertThat(action.getLocalStoragePath()).isEqualTo("/var/allure");
        assertThat(action.isDownloadEnabled()).isTrue();
        assertThat(action.isEnabledByDefault()).isTrue();
        assertThat(action.isCustomLogoEnabled()).isTrue();
        assertThat(action.isEnabledReportsCleanup()).isTrue();
    }

    @Test
    public void itShouldValidateRequiredFields() {
        final ConfigureAllureReportAction action = new TestConfigureAllureReportAction(settingsManager);
        action.setDownloadBaseUrl(" ");
        action.setLocalStoragePath(" ");

        action.validate();

        assertThat(action.getActionErrors())
                .containsExactly("allure.config.download.url.error.required", "allure.config.local.storage.required");
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
        assertThat(result).isEqualTo("success");
        assertThat(captor.getValue().isDownloadEnabled()).isFalse();
        assertThat(captor.getValue().getDownloadBaseUrl()).isEqualTo("https://downloads.example/");
        assertThat(captor.getValue().getLocalStoragePath()).isEqualTo("/tmp/allure");
    }

    @Test
    public void itShouldAnnotateEveryFormSetterForStrutsParameterInjection() {
        // Struts 7 (Bamboo 12) injects request parameters only into setters annotated with
        // @StrutsParameter; a bare setter silently receives nothing and the form save breaks.
        final List<Method> formSetters = Arrays.stream(ConfigureAllureReportAction.class.getDeclaredMethods())
                .filter(method -> method.getName().startsWith("set"))
                .toList();

        assertThat(formSetters).isNotEmpty();
        assertThat(formSetters)
                .allSatisfy(
                        setter -> assertThat(setter.getAnnotation(StrutsParameter.class))
                                .as("form setter %s must be annotated with @StrutsParameter", setter.getName())
                                .isNotNull()
                );
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
