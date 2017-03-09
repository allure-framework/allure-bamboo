package io.qameta.allure.bamboo;

import com.atlassian.bamboo.plan.cache.ImmutablePlan;
import com.atlassian.bamboo.plan.configuration.MiscellaneousPlanConfigurationPlugin;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.v2.build.BaseConfigurablePlugin;
import com.atlassian.bamboo.v2.build.ImportExportAwarePlugin;
import com.atlassian.bamboo.ww2.actions.build.admin.create.BuildConfiguration;
import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static com.atlassian.bamboo.plan.PlanClassHelper.isChain;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_ENABLED;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_EXECUTABLE;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_FAILED_ONLY;
import static org.apache.commons.lang.StringUtils.isEmpty;

@SuppressWarnings("unchecked")
public class AllureBuildConfigurator
        extends BaseConfigurablePlugin
        implements MiscellaneousPlanConfigurationPlugin, ImportExportAwarePlugin {

    @Override
    public boolean isApplicableTo(@NotNull final ImmutablePlan plan) {
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

    @NotNull
    @Override
    public Set<String> getConfigurationKeys() {
        return ImmutableSet.of(ALLURE_CONFIG_ENABLED, ALLURE_CONFIG_EXECUTABLE, ALLURE_CONFIG_FAILED_ONLY);
    }
}
