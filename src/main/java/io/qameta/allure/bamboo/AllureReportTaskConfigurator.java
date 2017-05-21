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


    public AllureReportTaskConfigurator(final TextProvider textProvider,
                                        final ArtifactDefinitionManager artifactDefinitionManager,
                                        final UIConfigSupport uiConfigSupport) {
        this.textProvider = textProvider;
        this.artifactDefinitionManager = artifactDefinitionManager;
        this.uiConfigSupport = uiConfigSupport;
    }

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull final ActionParametersMap params,
                                                     @Nullable final TaskDefinition previousTaskDefinition) {
        final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);
        config.put(ALLURE_RESULTS_DIRECTORY, params.getString(ALLURE_RESULTS_DIRECTORY));
        config.put(ALLURE_REPORT_DIRECTORY, params.getString(ALLURE_REPORT_DIRECTORY));
        config.put(ALLURE_CONFIG_EXECUTABLE, params.getString(ALLURE_CONFIG_EXECUTABLE));
        return config;
    }

    @Override
    public void populateContextForCreate(@NotNull final Map<String, Object> context) {
        super.populateContextForCreate(context);
        context.put(UI_CONFIG_BEAN, this.uiConfigSupport);
        context.put(ALLURE_RESULTS_DIRECTORY, "allure-results");
        context.put(ALLURE_REPORT_DIRECTORY, "allure-report");
    }

    @Override
    public void populateContextForEdit(@NotNull final Map<String, Object> context,
                                       @NotNull final TaskDefinition taskDefinition) {
        super.populateContextForEdit(context, taskDefinition);
        context.put(UI_CONFIG_BEAN, this.uiConfigSupport);
        context.put(ALLURE_RESULTS_DIRECTORY, taskDefinition.getConfiguration().get(ALLURE_RESULTS_DIRECTORY));
        context.put(ALLURE_REPORT_DIRECTORY, taskDefinition.getConfiguration().get(ALLURE_REPORT_DIRECTORY));
        context.put(ALLURE_CONFIG_EXECUTABLE, taskDefinition.getConfiguration().get(ALLURE_CONFIG_EXECUTABLE));
    }

    @Override
    public void validate(@NotNull final ActionParametersMap params, @NotNull final ErrorCollection errorCollection) {
        super.validate(params, errorCollection);

        validateNotEmpty(params, ALLURE_RESULTS_DIRECTORY, errorCollection);
        validateNotEmpty(params, ALLURE_REPORT_DIRECTORY, errorCollection);
        validateRelative(params, ALLURE_RESULTS_DIRECTORY, errorCollection);
        validateRelative(params, ALLURE_REPORT_DIRECTORY, errorCollection);
        validateNotEmpty(params, ALLURE_CONFIG_EXECUTABLE, errorCollection);
    }

    /**
     * Validate the property with given key are exists and not empty.
     *
     * @param params          the properties map to find the validated property by key.
     * @param key             the key of property to validate.
     * @param errorCollection the list of problems to add problem if needed.
     */
    private void validateNotEmpty(final ActionParametersMap params, final String key,
                                  final ErrorCollection errorCollection) {
        String value = params.getString(key);
        if (StringUtils.isEmpty(value)) {
            errorCollection.addError(key, textProvider.getText("error.property.empty"));
        }
    }

    /**
     * Validate the value of the property with given key. The validated value should be valid
     * relative path.
     *
     * @param params          the properties map to find the validated property by key.
     * @param key             the key of property to validate.
     * @param errorCollection the list of problems to add problem if needed.
     */
    private void validateRelative(final ActionParametersMap params, final String key,
                                  final ErrorCollection errorCollection) {
        String value = params.getString(key);
        if (StringUtils.isEmpty(value) || Paths.get(value).isAbsolute()) {
            errorCollection.addError(key, textProvider.getText("error.path.absolute"));
        }
    }

    @NotNull
    @Override
    public Set<Requirement> calculateRequirements(@NotNull final TaskDefinition taskDefinition,
                                                  @NotNull final Job job) {
        final String ARTIFACT_COPY_PATTERN = "**";
        if (null == artifactDefinitionManager.findArtifactDefinition(job, ALLURE_ARTIFACT_NAME)) {
            ArtifactDefinitionImpl artifactDefinition = new ArtifactDefinitionImpl(ALLURE_ARTIFACT_NAME,
                    taskDefinition.getConfiguration().get(ALLURE_REPORT_DIRECTORY), ARTIFACT_COPY_PATTERN);
            artifactDefinition.setProducerJob(job);
            artifactDefinitionManager.saveArtifactDefinition(artifactDefinition);
        }
        String key = ALLURE_EXECUTION_PREFIX + "." + taskDefinition.getConfiguration().get(ALLURE_CONFIG_EXECUTABLE);
        HashSet<Requirement> requirements = Sets.newHashSet();
        requirements.add(new RequirementImpl(key, true, ".*"));
        return requirements;
    }
}
