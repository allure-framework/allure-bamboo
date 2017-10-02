package io.qameta.allure.bamboo;

import com.atlassian.bamboo.build.BuildDefinition;
import com.atlassian.bamboo.chains.Chain;
import com.atlassian.bamboo.chains.ChainExecution;
import com.atlassian.bamboo.chains.ChainResultsSummary;
import com.atlassian.bamboo.chains.plugins.PostChainAction;
import com.atlassian.bamboo.configuration.AdministrationConfiguration;
import com.atlassian.bamboo.resultsummary.ResultsSummaryManager;
import com.atlassian.bamboo.v2.build.BaseConfigurablePlugin;
import com.atlassian.spring.container.ContainerManager;
import io.qameta.allure.bamboo.info.AddExecutorInfo;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static com.google.common.io.Files.createTempDir;
import static io.qameta.allure.bamboo.AllureBuildResult.allureBuildResult;
import static io.qameta.allure.bamboo.util.ExceptionUtil.stackTraceToString;
import static org.apache.commons.io.FileUtils.deleteQuietly;

@SuppressWarnings("ConstantConditions")
public class AllureBuildCompleteAction extends BaseConfigurablePlugin implements PostChainAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureBuildCompleteAction.class);

    private final AllureExecutableProvider allureExecutable;
    private final AllureSettingsManager settingsManager;
    private final AllureArtifactsManager artifactsManager;
    private final BambooExecutablesManager executablesManager;
    private final ResultsSummaryManager resulsSummaryManager;


    public AllureBuildCompleteAction(AllureExecutableProvider allureExecutable,
                                     AllureSettingsManager settingsManager,
                                     AllureArtifactsManager artifactsManager,
                                     BambooExecutablesManager executablesManager,
                                     ResultsSummaryManager resultsSummaryManager) {
        this.allureExecutable = allureExecutable;
        this.settingsManager = settingsManager;
        this.artifactsManager = artifactsManager;
        this.executablesManager = executablesManager;
        this.resulsSummaryManager = resultsSummaryManager;
    }

    @Override
    public void execute(@NotNull Chain chain, @NotNull ChainResultsSummary chainResultsSummary, @NotNull ChainExecution chainExecution) throws Exception {
        final BuildDefinition buildDef = chain.getBuildDefinition();
        final AllureGlobalConfig globalConfig = settingsManager.getSettings();
        final AllureBuildConfig buildConfig = AllureBuildConfig.fromContext(buildDef.getCustomConfiguration());
        final boolean allureEnabled = buildConfig.isEnabled() || (!buildConfig.isEnabledSet() && globalConfig.isEnabledByDefault());
        final boolean isEnabledForFailedOnly = buildConfig.isOnlyForFailed();
        if (!allureEnabled || (isEnabledForFailedOnly && !chainResultsSummary.isFailed())) {
            return;
        }
        final File artifactsTempDir = createTempDir();
        final File allureReportDir = new File(createTempDir(), "report");
        final Map<String, String> customBuildData = chainResultsSummary.getCustomBuildData();
        try {

            final String executable = Optional.ofNullable(buildConfig.getExecutable())
                    .orElse(executablesManager.getDefaultAllureExecutable()
                            .orElseThrow(() -> new RuntimeException("Could not find default Allure executable!" +
                                    " Please configure plugin properly!")));

            LOGGER.info("Allure Report is enabled for {}", chain.getName());
            LOGGER.info("Trying to get executable by name {} for {}", executable, chain.getName());

            AllureExecutable allure = allureExecutable.provide(globalConfig, executable).orElseThrow(() ->
                    new RuntimeException("Failed to find Allure executable by name " + executable));

            LOGGER.info("Starting artifacts downloading into {} for {}", artifactsTempDir.getPath(), chain.getName());
            artifactsManager.downloadAllArtifactsTo(chainResultsSummary, artifactsTempDir);
            if (artifactsTempDir.list().length == 0) {
                allureBuildResult(false, "Build result does not have any uploaded artifacts!")
                        .dumpToCustomData(customBuildData);
            } else {
                LOGGER.info("Starting allure generate into {} for {}", allureReportDir.getPath(), chain.getName());
                prepareResults(artifactsTempDir, chain, chainExecution);
                allure.generate(artifactsTempDir.toPath(), allureReportDir.toPath());
                LOGGER.info("Allure has been generated successfully for {}", chain.getName());
                artifactsManager.uploadReportArtifacts(chain, chainResultsSummary, allureReportDir)
                            .ifPresent(result -> result.dumpToCustomData(customBuildData));

            }
        } catch (Exception e) {
            LOGGER.error("Failed to build allure report for {}", chain.getName(), e);
            allureBuildResult(false, stackTraceToString(e)).dumpToCustomData(customBuildData);
        } finally {
            deleteQuietly(artifactsTempDir);
            deleteQuietly(allureReportDir);
        }
    }

    private void prepareResults(File artifactsTempDir, Chain chain, ChainExecution chainExecution) throws IOException, InterruptedException {
        addExecutorInfo(artifactsTempDir, chain, chainExecution);
    }

    private void addExecutorInfo(File artifactsTempDir, Chain chain, ChainExecution chainExecution) throws IOException, InterruptedException {
        String rootUrl = getAdministrationConfiguration().getBaseUrl();
        String buildId = chainExecution.getPlanResultKey().getBuildNumber() + "";
        String buildName = chain.getBuildName();
        String buildUrl = String.format("%s/browse/%s-%s", rootUrl, chain.getPlanKey().getKey(), buildId);
        String reportUrl = String.format("%s/plugins/servlet/allure/report/%s/%s/", rootUrl,
                chain.getPlanKey().getKey(), buildId);
        new AddExecutorInfo(rootUrl, buildId, buildName, buildUrl, reportUrl).invoke(artifactsTempDir);
    }

    private AdministrationConfiguration getAdministrationConfiguration() {
        return (AdministrationConfiguration) ContainerManager.getComponent("administrationConfiguration");
    }
}
