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
package io.qameta.allure.bamboo.config;

import com.atlassian.bamboo.build.BuildDefinitionManager;
import com.atlassian.bamboo.build.artifact.ArtifactLinkManager;
import com.atlassian.bamboo.build.artifact.handlers.ArtifactHandlersService;
import com.atlassian.bamboo.resultsummary.ResultsSummaryManager;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilitySetManager;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import io.qameta.allure.bamboo.AllureArtifactsManager;
import io.qameta.allure.bamboo.AllureCommandLineSupport;
import io.qameta.allure.bamboo.AllureDownloader;
import io.qameta.allure.bamboo.AllureExecutableProvider;
import io.qameta.allure.bamboo.AllurePluginInstallTask;
import io.qameta.allure.bamboo.AllureSettingsManager;
import io.qameta.allure.bamboo.BambooExecutablesManager;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;

import static io.qameta.allure.bamboo.TestSupport.attachText;
import static io.qameta.allure.bamboo.TestSupport.step;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.junit.MockitoJUnit.rule;

public class AllurePluginJavaConfigTest {

    @Rule
    public MockitoRule mockitoRule = rule();

    @Mock
    private PluginSettingsFactory pluginSettingsFactory;
    @Mock
    private PluginSettings pluginSettings;
    @Mock
    private CapabilitySetManager capabilitySetManager;
    @Mock
    private PluginAccessor pluginAccessor;
    @Mock
    private ArtifactHandlersService artifactHandlersService;
    @Mock
    private BuildDefinitionManager buildDefinitionManager;
    @Mock
    private ResultsSummaryManager resultsSummaryManager;
    @Mock
    private ArtifactLinkManager artifactLinkManager;
    @Mock
    private ApplicationProperties applicationProperties;

    @Test
    public void itShouldConstructTheCoreBeans() throws Exception {
        when(pluginSettingsFactory.createGlobalSettings()).thenReturn(pluginSettings);
        final AllurePluginJavaConfig config = step("create the plugin Java configuration",
                AllurePluginJavaConfig::new);
        final AllureCommandLineSupport commandLineSupport = step("create the command line support bean",
                config::allureCommandLineSupport);
        final AllureSettingsManager settingsManager = step("create the global settings manager",
                () -> config.allureSettings(pluginSettingsFactory));
        final AllureDownloader downloader = step("create the Allure downloader bean",
                () -> config.allureDownloader(settingsManager));
        final BambooExecutablesManager executablesManager = step("create the Bamboo executables manager",
                () -> config.bambooExecutableManager(capabilitySetManager));
        final AllureExecutableProvider executableProvider = step("create the executable provider",
                () -> config.allureExecutableProvider(executablesManager, downloader, commandLineSupport));
        final AllureArtifactsManager artifactsManager = step("create the artifacts manager",
                () -> config.allureArtifactsUploader(
                        pluginAccessor,
                        artifactHandlersService,
                        buildDefinitionManager,
                        resultsSummaryManager,
                        artifactLinkManager,
                        applicationProperties,
                        settingsManager));
        final AllurePluginInstallTask installTask = step("create the plugin install task",
                () -> (AllurePluginInstallTask) config.allureInstallTask(executablesManager));

        step("verify each configuration entry returns the expected implementation", () -> {
            attachText("Constructed bean types",
                    String.join("\n",
                            commandLineSupport.getClass().getName(),
                            settingsManager.getClass().getName(),
                            downloader.getClass().getName(),
                            executablesManager.getClass().getName(),
                            executableProvider.getClass().getName(),
                            artifactsManager.getClass().getName(),
                            installTask.getClass().getName()));
            assertThat(commandLineSupport).isNotNull();
            assertThat(settingsManager).isInstanceOf(AllureSettingsManager.class);
            assertThat(downloader).isInstanceOf(AllureDownloader.class);
            assertThat(executablesManager).isInstanceOf(BambooExecutablesManager.class);
            assertThat(executableProvider).isInstanceOf(AllureExecutableProvider.class);
            assertThat(artifactsManager).isInstanceOf(AllureArtifactsManager.class);
            assertThat(installTask).isInstanceOf(AllurePluginInstallTask.class);
        });
    }
}
