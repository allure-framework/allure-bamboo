package ru.yandex.qatools.allure.bamboo;

import com.atlassian.bamboo.build.Job;
import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.plan.artifact.ArtifactDefinitionImpl;
import com.atlassian.bamboo.plan.artifact.ArtifactDefinitionManager;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.BuildTaskRequirementSupport;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.v2.build.agent.capability.Requirement;
import com.atlassian.struts.TextProvider;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static ru.yandex.qatools.allure.bamboo.AllureTaskConstants.GLOB;
import static ru.yandex.qatools.allure.bamboo.AllureTaskConstants.GLOB_DEFAULT;
import static ru.yandex.qatools.allure.bamboo.AllureTaskConstants.GLOB_ERROR;
import static ru.yandex.qatools.allure.bamboo.AllureTaskConstants.VERSION;
import static ru.yandex.qatools.allure.bamboo.AllureTaskConstants.VERSION_DEFAULT;
import static ru.yandex.qatools.allure.bamboo.AllureTaskConstants.VERSION_ERROR;

public class AllureTaskConfigurator extends AbstractTaskConfigurator implements BuildTaskRequirementSupport {
    
    private TextProvider textProvider;
    private ArtifactDefinitionManager artifactDefinitionManager;
    
    public AllureTaskConfigurator(TextProvider textProvider, ArtifactDefinitionManager artifactDefinitionManager) {
        this.textProvider = textProvider;
        this.artifactDefinitionManager = artifactDefinitionManager;
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

    @NotNull
    @Override
    public Set<Requirement> calculateRequirements(@NotNull TaskDefinition taskDefinition, @NotNull Job job) {
        final String ARTIFACT_NAME = "Allure Report";
        final String ARTIFACT_COPY_PATTERN = "**";
        if (null == artifactDefinitionManager.findArtifactDefinition(job, ARTIFACT_NAME)) {
            ArtifactDefinitionImpl artifactDefinition = new ArtifactDefinitionImpl(ARTIFACT_NAME, AllureTask.RELATIVE_OUTPUT_DIRECTORY, ARTIFACT_COPY_PATTERN);
            artifactDefinition.setProducerJob(job);
            artifactDefinitionManager.saveArtifactDefinition(artifactDefinition);
        }
        return Collections.emptySet();
    }
}
