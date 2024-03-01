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

import com.atlassian.bamboo.plan.cache.ImmutablePlan;
import com.atlassian.bamboo.plan.configuration.MiscellaneousPlanConfigurationPlugin;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.v2.build.BaseConfigurablePlugin;
import com.atlassian.bamboo.ww2.actions.build.admin.create.BuildConfiguration;
import org.jetbrains.annotations.NotNull;

import static com.atlassian.bamboo.plan.PlanClassHelper.isChain;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_ENABLED;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_EXECUTABLE;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_FAILED_ONLY;
import static java.lang.Boolean.TRUE;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class AllureBuildConfigurator extends BaseConfigurablePlugin
        implements MiscellaneousPlanConfigurationPlugin {

    private BambooExecutablesManager executablesManager;

    private AllureSettingsManager settingsManager;

    @Override
    public boolean isApplicableTo(final @NotNull ImmutablePlan plan) {
        return isChain(plan);
    }

    @NotNull
    @Override
    public ErrorCollection validate(final @NotNull BuildConfiguration buildConfiguration) {
        final ErrorCollection collection = super.validate(buildConfiguration);
        if (buildConfiguration.getBoolean(ALLURE_CONFIG_ENABLED)
                && isEmpty(buildConfiguration.getString(ALLURE_CONFIG_EXECUTABLE))) {
            collection.addError(ALLURE_CONFIG_EXECUTABLE, "Cannot be empty!");
        }
        return collection;
    }

    @Override
    public void prepareConfigObject(final @NotNull BuildConfiguration buildConfiguration) {
        super.prepareConfigObject(buildConfiguration);
        if (buildConfiguration.getProperty(ALLURE_CONFIG_ENABLED) == null) {
            ofNullable(settingsManager).map(AllureSettingsManager::getSettings).ifPresent(settings ->
                    buildConfiguration.setProperty(ALLURE_CONFIG_ENABLED, settings.isEnabledByDefault()));
        }
        if (buildConfiguration.getProperty(ALLURE_CONFIG_FAILED_ONLY) == null) {
            buildConfiguration.setProperty(ALLURE_CONFIG_FAILED_ONLY, TRUE);
        }
        if (buildConfiguration.getProperty(ALLURE_CONFIG_EXECUTABLE) == null) {
            ofNullable(executablesManager).flatMap(BambooExecutablesManager::getDefaultAllureExecutable)
                    .ifPresent(executable -> buildConfiguration.setProperty(ALLURE_CONFIG_EXECUTABLE, executable));
        }
    }

    public void setSettingsManager(final AllureSettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    public void setExecutablesManager(final BambooExecutablesManager executablesManager) {
        this.executablesManager = executablesManager;
    }
}
