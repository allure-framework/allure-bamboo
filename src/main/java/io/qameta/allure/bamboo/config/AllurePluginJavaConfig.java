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
import com.atlassian.bamboo.comment.CommentService;
import com.atlassian.bamboo.deployments.projects.service.DeploymentProjectService;
import com.atlassian.bamboo.logger.ErrorAccessor;
import com.atlassian.bamboo.resultsummary.ResultsSummaryManager;
import com.atlassian.bamboo.template.TemplateRenderer;
import com.atlassian.bamboo.user.BambooAuthenticationContext;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilitySetManager;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugins.osgi.javaconfig.configs.beans.ModuleFactoryBean;
import com.atlassian.plugins.osgi.javaconfig.configs.beans.PluginAccessorBean;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.upgrade.PluginUpgradeTask;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.struts.TextProvider;
import io.qameta.allure.bamboo.AllureArtifactsManager;
import io.qameta.allure.bamboo.AllureCommandLineSupport;
import io.qameta.allure.bamboo.AllureDownloader;
import io.qameta.allure.bamboo.AllureExecutableProvider;
import io.qameta.allure.bamboo.AllurePluginInstallTask;
import io.qameta.allure.bamboo.AllureSettingsManager;
import io.qameta.allure.bamboo.BambooExecutablesManager;
import org.osgi.framework.ServiceRegistration;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static com.atlassian.plugins.osgi.javaconfig.OsgiServices.exportOsgiService;
import static com.atlassian.plugins.osgi.javaconfig.OsgiServices.importOsgiService;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.CouplingBetweenObjects"})
@Configuration
@Import({
        ModuleFactoryBean.class,
        PluginAccessorBean.class
})
public class AllurePluginJavaConfig {


    // <component-import key="applicationProperties" interface="com.atlassian.sal.api.ApplicationProperties"/>
    @Bean
    public ApplicationProperties applicationProperties() {
        return importOsgiService(ApplicationProperties.class);
    }

    // <component-import key="textProvider" interface="com.atlassian.struts.TextProvider"/>
    @Bean
    public TextProvider textProvider() {
        return importOsgiService(TextProvider.class);
    }

    // <component-import key="artifactLinkManager"
    //                   interface="com.atlassian.bamboo.build.artifact.ArtifactLinkManager"/>
    @Bean
    public ArtifactLinkManager artifactLinkManager() {
        return importOsgiService(ArtifactLinkManager.class);
    }

    // <component-import key="capabilitySetManager"
    //                   interface="com.atlassian.bamboo.v2.build.agent.capability.CapabilitySetManager"/>
    @Bean
    public CapabilitySetManager capabilitySetManager() {
        return importOsgiService(CapabilitySetManager.class);
    }

    // <component-import key="resultsSummaryManager"
    //                   interface="com.atlassian.bamboo.resultsummary.ResultsSummaryManager"/>
    @Bean
    public ResultsSummaryManager resultsSummaryManager() {
        return importOsgiService(ResultsSummaryManager.class);
    }

    // <component-import key="errorAccessor" interface="com.atlassian.bamboo.logger.ErrorAccessor"/>
    @Bean
    public ErrorAccessor errorAccessor() {
        return importOsgiService(ErrorAccessor.class);
    }

    // <component-import key="commentsService" interface="com.atlassian.bamboo.comment.CommentService"/>
    @Bean
    public CommentService commentsService() {
        return importOsgiService(CommentService.class);
    }

    // <component-import key="deploymentProjectService"
    //                   interface="com.atlassian.bamboo.deployments.projects.service.DeploymentProjectService"/>
    @Bean
    public DeploymentProjectService deploymentProjectService() {
        return importOsgiService(DeploymentProjectService.class);
    }

    // <component-import key="buildDefinitionManager"
    //                   interface="com.atlassian.bamboo.build.BuildDefinitionManager"/>
    @Bean
    public BuildDefinitionManager buildDefinitionManager() {
        return importOsgiService(BuildDefinitionManager.class);
    }

    // <component-import key="renderer" interface="com.atlassian.templaterenderer.TemplateRenderer"/>
    @Bean
    public TemplateRenderer renderer() {
        return importOsgiService(TemplateRenderer.class);
    }

    // <component-import key="loginUriProvider" interface="com.atlassian.sal.api.auth.LoginUriProvider"/>
    @Bean
    public LoginUriProvider loginUriProvider() {
        return importOsgiService(LoginUriProvider.class);
    }

    // <component-import key="userManager" interface="com.atlassian.sal.api.user.UserManager"/>
    @Bean
    public UserManager userManager() {
        return importOsgiService(UserManager.class);
    }

    @Bean
    public BambooAuthenticationContext bambooAuthenticationContext() {
        return importOsgiService(BambooAuthenticationContext.class);
    }

    // <component-import key="pluginSettingsFactory"
    //                   interface="com.atlassian.sal.api.pluginsettings.PluginSettingsFactory"/>
    @Bean
    public PluginSettingsFactory pluginSettingsFactory() {
        return importOsgiService(PluginSettingsFactory.class);
    }

    // <component-import key="pluginAccessor" interface="com.atlassian.plugin.PluginAccessor"/>
    @Bean
    public PluginAccessor pluginAccessor() {
        return importOsgiService(PluginAccessor.class);
    }

    // <component-import key="artifactHandlersService"
    //                   interface="com.atlassian.bamboo.build.artifact.handlers.ArtifactHandlersService"/>
    @Bean
    public ArtifactHandlersService artifactHandlersService() {
        return importOsgiService(ArtifactHandlersService.class);
    }

    //    <component key="allureCommandLineSupport" name="Allure Command Line Support"
    //               class="io.qameta.allure.bamboo.AllureCommandLineSupport"/>
    @Bean
    public AllureCommandLineSupport allureCommandLineSupport() {
        return new AllureCommandLineSupport();
    }

    //    <component key="allureSettings" name="Allure Settings"
    //                                    class="io.qameta.allure.bamboo.AllureSettingsManager"/>
    @Bean
    public AllureSettingsManager allureSettings(final PluginSettingsFactory pluginSettingsFactory) {
        return new AllureSettingsManager(pluginSettingsFactory);
    }

    //    <component key="allureDownloader" name="Allure Downloader"
    //                                      class="io.qameta.allure.bamboo.AllureDownloader"/>
    @Bean
    public AllureDownloader allureDownloader(final AllureSettingsManager allureSettingsManager) {
        return new AllureDownloader(allureSettingsManager);
    }

    //    <component key="bambooExecutableManager" name="Bamboo Executable Manager"
    //               class="io.qameta.allure.bamboo.BambooExecutablesManager"/>
    @Bean
    public BambooExecutablesManager bambooExecutableManager(final CapabilitySetManager capabilitySetManager) {
        return new BambooExecutablesManager(capabilitySetManager);
    }

    //    <component key="allureExecutableProvider" name="Allure Executable Provider"
    //               class="io.qameta.allure.bamboo.AllureExecutableProvider"/>
    @Bean
    public AllureExecutableProvider allureExecutableProvider(final BambooExecutablesManager bambooExecutableManager,
                                                             final AllureDownloader allureDownloader,
                                                             final AllureCommandLineSupport allureCommandLineSupport) {
        return new AllureExecutableProvider(
                bambooExecutableManager,
                allureDownloader,
                allureCommandLineSupport
        );
    }

    //    <component key="allureArtifactsUploader" name="Allure Artifacts Uploader"
    //               class="io.qameta.allure.bamboo.AllureArtifactsManager"/>
    @Bean
    public AllureArtifactsManager allureArtifactsUploader(final PluginAccessor pluginAccessor,
                                                          final ArtifactHandlersService artifactHandlersService,
                                                          final BuildDefinitionManager buildDefinitionManager,
                                                          final ResultsSummaryManager resultsSummaryManager,
                                                          final ArtifactLinkManager artifactLinkManager,
                                                          final ApplicationProperties applicationProperties,
                                                          final AllureSettingsManager allureSettingsManager) {
        return new AllureArtifactsManager(
                pluginAccessor,
                artifactHandlersService,
                buildDefinitionManager,
                resultsSummaryManager,
                artifactLinkManager,
                applicationProperties,
                allureSettingsManager
        );
    }

    //    <component key="allureInstallTask" name="Allure Install Task"
    //               class="io.qameta.allure.bamboo.AllurePluginInstallTask" public="true">
    //        <interface>com.atlassian.sal.api.upgrade.PluginUpgradeTask</interface>
    //    </component>
    @Bean
    public PluginUpgradeTask allureInstallTask(final BambooExecutablesManager bambooExecutableManager) {
        return new AllurePluginInstallTask(bambooExecutableManager);
    }

    // Exports MyPluginComponent as an OSGi service
    @Bean
    public FactoryBean<ServiceRegistration> registerMyDelegatingService(
            final PluginUpgradeTask allureInstallTask) {
        return exportOsgiService(allureInstallTask, null, PluginUpgradeTask.class);
    }
}
