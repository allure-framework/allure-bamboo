package ru.yandex.qatools.allure.bamboo;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.struts.TextProvider;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static ru.yandex.qatools.allure.bamboo.AllureTaskConstants.GLOB;
import static ru.yandex.qatools.allure.bamboo.AllureTaskConstants.GLOB_DEFAULT;
import static ru.yandex.qatools.allure.bamboo.AllureTaskConstants.GLOB_ERROR;
import static ru.yandex.qatools.allure.bamboo.AllureTaskConstants.VERSION;
import static ru.yandex.qatools.allure.bamboo.AllureTaskConstants.VERSION_DEFAULT;
import static ru.yandex.qatools.allure.bamboo.AllureTaskConstants.VERSION_ERROR;

public class AllureTaskConfigurator extends AbstractTaskConfigurator {
    
    private TextProvider textProvider;
    
    public AllureTaskConfigurator(final TextProvider textProvider) {
        this.textProvider = textProvider;
    }

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull ActionParametersMap params, @Nullable TaskDefinition previousTaskDefinition) {
        final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);
        config.put(GLOB, params.getString(GLOB));
        config.put(VERSION, params.getString(VERSION));
        return config;
    }

    @Override
    public void populateContextForCreate(@NotNull final Map<String, Object> context) {
        super.populateContextForCreate(context);
        context.put(GLOB, GLOB_DEFAULT);
        context.put(VERSION, VERSION_DEFAULT);
    }

    @Override
    public void populateContextForEdit(@NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition) {
        super.populateContextForEdit(context, taskDefinition);
        context.put(GLOB, taskDefinition.getConfiguration().get(GLOB));
        context.put(VERSION, taskDefinition.getConfiguration().get(VERSION));
    }

    @Override
    public void populateContextForView(@NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition) {
        super.populateContextForView(context, taskDefinition);
        context.put(GLOB, taskDefinition.getConfiguration().get(GLOB));
        context.put(VERSION, taskDefinition.getConfiguration().get(VERSION));
    }

    @Override
    public void validate(@NotNull ActionParametersMap params, @NotNull ErrorCollection errorCollection) {
        super.validate(params, errorCollection);
        
        final String glob = params.getString(GLOB);
        if (StringUtils.isEmpty(glob)) {
            errorCollection.addError(GLOB, textProvider.getText(GLOB_ERROR));
        }
        
        final String version = params.getString(VERSION);
        if (StringUtils.isEmpty(version)) {
            errorCollection.addError(VERSION, textProvider.getText(VERSION_ERROR));
        }
    }

}
