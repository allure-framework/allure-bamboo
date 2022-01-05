package io.qameta.allure.bamboo;

import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.v2.build.BaseConfigurablePlugin;
import com.atlassian.bamboo.v2.build.configuration.MiscellaneousBuildConfigurationPlugin;
import com.atlassian.bamboo.ww2.actions.build.admin.create.BuildConfiguration;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

 import static com.atlassian.bamboo.plan.PlanClassHelper.isChain;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_ENABLED;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_EXECUTABLE;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_FAILED_ONLY;
import static java.lang.Boolean.TRUE;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang.StringUtils.isEmpty;

@SuppressWarnings("unchecked")
public class AllureBuildConfigurator extends BaseConfigurablePlugin
        implements MiscellaneousPlanConfigurationPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureBuildConfigurator.class);

    private BambooExecutablesManager executablesManager;

    private AllureSettingsManager settingsManager;

    @Override
    public boolean isApplicableTo(@NotNull ImmutablePlan immutablePlan) {
        return isChain(immutablePlan);
    }

    @Override
    public boolean isApplicableTo(@NotNull final Plan plan) {
        return isChain(plan);
    }

    @NotNull
    @Override
    public ErrorCollection validate(@NotNull BuildConfiguration buildConfiguration) {
        final ErrorCollection collection = super.validate(buildConfiguration);
        if (buildConfiguration.getBoolean(ALLURE_CONFIG_ENABLED)) {
            if (isEmpty(buildConfiguration.getString(ALLURE_CONFIG_EXECUTABLE))) {
                collection.addError(ALLURE_CONFIG_EXECUTABLE, "Cannot be empty!");
            }
        }
        return collection;
    }

    @Override
    public void prepareConfigObject(@NotNull BuildConfiguration buildConfiguration) {
        super.prepareConfigObject(buildConfiguration);
        if (buildConfiguration.getProperty(ALLURE_CONFIG_ENABLED) == null) {
            ofNullable(settingsManager).map(AllureSettingsManager::getSettings).ifPresent(settings ->
                    buildConfiguration.setProperty(ALLURE_CONFIG_ENABLED, settings.isEnabledByDefault()));
        }
        if (buildConfiguration.getProperty(ALLURE_CONFIG_FAILED_ONLY) == null) {
            buildConfiguration.setProperty(ALLURE_CONFIG_FAILED_ONLY, TRUE);
        }
        if (buildConfiguration.getProperty(ALLURE_CONFIG_EXECUTABLE) == null) {
            ofNullable(executablesManager).map(manager -> manager.getDefaultAllureExecutable().orElse(null))
                    .ifPresent(executable -> buildConfiguration.setProperty(ALLURE_CONFIG_EXECUTABLE, executable));
        }
    }

    public void setSettingsManager(AllureSettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    public void setExecutablesManager(BambooExecutablesManager executablesManager) {
        this.executablesManager = executablesManager;
    }
}
