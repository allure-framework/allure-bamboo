package io.qameta.allure.bamboo;

import com.atlassian.bamboo.build.Job;
import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.plan.artifact.ArtifactDefinitionImpl;
import com.atlassian.bamboo.plan.artifact.ArtifactDefinitionManager;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.BuildTaskRequirementSupport;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.v2.build.agent.capability.Requirement;
import com.atlassian.bamboo.v2.build.agent.capability.RequirementImpl;
import com.atlassian.bamboo.ww2.actions.build.admin.create.UIConfigSupport;
import com.atlassian.struts.TextProvider;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static io.qameta.allure.bamboo.AllureConstants.*;

/**
 * Provide task parameters.
 * Created by bvo2002 on 30.11.16.
 */
public class AllureReportTaskConfigurator extends AbstractTaskConfigurator implements BuildTaskRequirementSupport {

    private static final String UI_CONFIG_BEAN = "uiConfigBean";
    private TextProvider textProvider;
    private ArtifactDefinitionManager artifactDefinitionManager;
    private UIConfigSupport uiConfigSupport;


    public AllureReportTaskConfigurator(TextProvider textProvider, ArtifactDefinitionManager artifactDefinitionManager, UIConfigSupport uiConfigSupport) {
        this.textProvider = textProvider;
        this.artifactDefinitionManager = artifactDefinitionManager;
        this.uiConfigSupport = uiConfigSupport;
    }

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull ActionParametersMap params, @Nullable TaskDefinition previousTaskDefinition) {
        final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);
        config.put(RESULTS_DIRECTORY, params.getString(RESULTS_DIRECTORY));
        config.put(REPORT_DIRECTORY, params.getString(REPORT_DIRECTORY));
        config.put(EXECUTABLE_LABEL, params.getString(EXECUTABLE_LABEL));
        return config;
    }

    @Override
    public void populateContextForCreate(@NotNull final Map<String, Object> context) {
        super.populateContextForCreate(context);
        context.put(UI_CONFIG_BEAN, this.uiConfigSupport);
        context.put(RESULTS_DIRECTORY, RESULTS_DIRECTORY_DEFAULT);
        context.put(REPORT_DIRECTORY, REPORT_PATH_PREFIX_DEFAULT);
    }

    @Override
    public void populateContextForEdit(@NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition) {
        super.populateContextForEdit(context, taskDefinition);
        context.put(UI_CONFIG_BEAN, this.uiConfigSupport);
        context.put(RESULTS_DIRECTORY, taskDefinition.getConfiguration().get(RESULTS_DIRECTORY));
        context.put(REPORT_DIRECTORY, taskDefinition.getConfiguration().get(REPORT_DIRECTORY));
    }

    @Override
    public void validate(@NotNull ActionParametersMap params, @NotNull ErrorCollection errorCollection) {
        super.validate(params, errorCollection);

        validateNotEmpty(params, RESULTS_DIRECTORY, errorCollection);
        validateNotEmpty(params, REPORT_DIRECTORY, errorCollection);

        validateRelative(params, RESULTS_DIRECTORY, errorCollection);
        validateRelative(params, REPORT_DIRECTORY, errorCollection);
    }

    /**
     * Validate the property with given key are exists and not empty.
     *
     * @param params the properties map to find the validated property by key.
     * @param key        the key of property to validate.
     * @param errorCollection   the list of problems to add problem if needed.
     */
    private void validateNotEmpty(ActionParametersMap params, String key, ErrorCollection errorCollection) {
        String value = params.getString(key);
        if (StringUtils.isEmpty(value)) {
            errorCollection.addError(key, textProvider.getText("error.property.empty"));
        }
    }

    /**
     * Validate the value of the property with given key. The validated value should be valid
     * relative path.
     *
     * @param params the properties map to find the validated property by key.
     * @param key        the key of property to validate.
     * @param errorCollection   the list of problems to add problem if needed.
     */
    private void validateRelative(ActionParametersMap params, String key, ErrorCollection errorCollection) {
        String value = params.getString(key);
        if (StringUtils.isEmpty(value) || Paths.get(value).isAbsolute()) {
            errorCollection.addError(key, textProvider.getText("error.path.absolute"));
        }
    }

    @NotNull
    @Override
    public Set<Requirement> calculateRequirements(@NotNull TaskDefinition taskDefinition, @NotNull Job job) {
        final String ARTIFACT_COPY_PATTERN = "**";
        if (null == artifactDefinitionManager.findArtifactDefinition(job, ARTIFACT_NAME)) {
            ArtifactDefinitionImpl artifactDefinition =
                    new ArtifactDefinitionImpl(ARTIFACT_NAME, taskDefinition.getConfiguration().get(REPORT_DIRECTORY), ARTIFACT_COPY_PATTERN);
            artifactDefinition.setProducerJob(job);
            artifactDefinitionManager.saveArtifactDefinition(artifactDefinition);
        }

        HashSet<Requirement> requirements = Sets.newHashSet();
        requirements.add(new RequirementImpl( AllureCapability.ALLURE_CAPABILITY_PREFIX + "." + taskDefinition.getConfiguration().get(EXECUTABLE_LABEL), true, ".*"));
        return requirements;
    }
}
