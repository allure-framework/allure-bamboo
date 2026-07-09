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

import com.atlassian.bamboo.template.TemplateRenderer;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.ww2.actions.build.admin.create.BuildConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;

import java.util.Optional;

import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_ENABLED;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_EXECUTABLE;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_FAILED_ONLY;
import static io.qameta.allure.bamboo.TestSupport.attachText;
import static io.qameta.allure.bamboo.TestSupport.step;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.junit.MockitoJUnit.rule;

@SuppressWarnings("checkstyle:MultipleStringLiterals")
public class AllureBuildConfiguratorTest {

    @Rule
    public MockitoRule mockitoRule = rule();

    @Mock
    private BambooExecutablesManager executablesManager;
    @Mock
    private AllureSettingsManager settingsManager;
    @Mock
    private TemplateRenderer templateRenderer;

    @Test
    public void itShouldValidateMissingExecutableWhenAllureIsEnabled() throws Exception {
        final AllureBuildConfigurator configurator = new AllureBuildConfigurator(
                executablesManager, settingsManager, templateRenderer
        );
        final BuildConfiguration configuration = new BuildConfiguration();
        configuration.setProperty(ALLURE_CONFIG_ENABLED, true);
        configuration.setProperty(ALLURE_CONFIG_EXECUTABLE, "");

        final ErrorCollection errors = step(
                "validate an enabled configuration without an executable",
                () -> configurator.validate(configuration)
        );

        step("verify validation reports the missing executable", () -> {
            attachText("Validation errors", errors.getErrors().toString());
            assertThat(errors.hasAnyErrors()).isTrue();
            assertThat(errors.getErrors().get(ALLURE_CONFIG_EXECUTABLE)).contains("Cannot be empty!");
        });
    }

    @Test
    public void itShouldPopulateDefaultsFromSettingsAndCapabilities() throws Exception {
        final AllureBuildConfigurator configurator = new AllureBuildConfigurator(
                executablesManager, settingsManager, templateRenderer
        );
        final BuildConfiguration configuration = new BuildConfiguration();
        when(settingsManager.getSettings()).thenReturn(
                new AllureGlobalConfig(
                        "true",
                        "true",
                        "https://downloads.example/",
                        "/tmp/allure",
                        "false",
                        "false"
                )
        );
        when(executablesManager.getDefaultAllureExecutable()).thenReturn(Optional.of("Allure 2.30.0"));

        step(
                "populate the build configuration from settings and capabilities",
                () -> configurator.prepareConfigObject(configuration)
        );

        step("verify the default flags and executable name were applied", () -> {
            attachText(
                    "Prepared configuration",
                    "enabled=" + configuration.getBoolean(ALLURE_CONFIG_ENABLED)
                            + "\nfailedOnly=" + configuration.getBoolean(ALLURE_CONFIG_FAILED_ONLY)
                            + "\nexecutable=" + configuration.getString(ALLURE_CONFIG_EXECUTABLE)
            );
            assertThat(configuration.getBoolean(ALLURE_CONFIG_ENABLED)).isTrue();
            assertThat(configuration.getBoolean(ALLURE_CONFIG_FAILED_ONLY)).isTrue();
            assertThat(configuration.getString(ALLURE_CONFIG_EXECUTABLE)).isEqualTo("Allure 2.30.0");
        });
    }
}
