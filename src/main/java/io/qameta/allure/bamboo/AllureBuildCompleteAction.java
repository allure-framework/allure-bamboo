package io.qameta.allure.bamboo;

import com.atlassian.bamboo.build.BuildDefinition;
import com.atlassian.bamboo.chains.Chain;
import com.atlassian.bamboo.chains.ChainExecution;
import com.atlassian.bamboo.chains.ChainResultsSummary;
import com.atlassian.bamboo.chains.plugins.PostChainAction;
import com.atlassian.bamboo.v2.build.BaseConfigurablePlugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

import static com.google.common.io.Files.createTempDir;
import static io.qameta.allure.bamboo.AllureBuildResult.allureBuildResult;
import static io.qameta.allure.bamboo.util.ExceptionUtil.stackTraceToString;
import static java.util.Optional.ofNullable;
import static org.apache.commons.io.FileUtils.deleteQuietly;

@SuppressWarnings("ConstantConditions")
public class AllureBuildCompleteAction extends BaseConfigurablePlugin implements PostChainAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureBuildCompleteAction.class);

    private final AllureExecutableProvider allureExecutable;
    private final AllureSettingsManager settingsManager;
    private final AllureArtifactsManager artifactsManager;
    private final BambooExecutablesManager executablesManager;


    public AllureBuildCompleteAction(AllureExecutableProvider allureExecutable,
                                     AllureSettingsManager settingsManager,
                                     AllureArtifactsManager artifactsManager,
                                     BambooExecutablesManager executablesManager) {
        this.allureExecutable = allureExecutable;
        this.settingsManager = settingsManager;
        this.artifactsManager = artifactsManager;
        this.executablesManager = executablesManager;
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
            final String executable = ofNullable(buildConfig.getExecutable()).orElseGet(() -> {
                LOGGER.info("Allure executable has not been configured. Using default one!");
                return executablesManager.getDefaultAllureExecutable().orElseThrow(() ->
                        new RuntimeException("Could not find default Allure executable! Please configure plugin properly!"));
            });
            LOGGER.info("Allure Report is enabled for {}", chain.getName());
            LOGGER.info("Trying to get executable by name {} for {}", executable, chain.getName());
            allureExecutable.provide(globalConfig, executable).map(allure -> {
                LOGGER.info("Starting artifacts downloading into {} for {}", artifactsTempDir.getPath(), chain.getName());
                artifactsManager.downloadAllArtifactsTo(chainResultsSummary, artifactsTempDir);
                if (artifactsTempDir.list().length == 0) {
                    allureBuildResult(false, "Build result does not have any uploaded artifacts!")
                            .dumpToCustomData(customBuildData);
                } else {
                    LOGGER.info("Starting allure generate into {} for {}", allureReportDir.getPath(), chain.getName());
                    final AllureGenerateResult genRes = allure.generate(artifactsTempDir.toPath(), allureReportDir.toPath());
                    if (!genRes.isContainsTestCases()) {
                        allureBuildResult(false, "No Allure results found! Please ensure that build artifacts contain " +
                                "Allure results!\nAllure generate output: \n" + genRes.getOutput()).dumpToCustomData(customBuildData);
                    } else {
                        LOGGER.info("Allure has been generated successfully for {}", chain.getName());
                        artifactsManager.uploadReportArtifacts(chain, chainResultsSummary, allureReportDir)
                                .ifPresent(result -> result.dumpToCustomData(customBuildData));
                    }
                }
                return true;
            }).orElseThrow(() -> new RuntimeException("Failed to find Allure executable by name " + executable));
        } catch (Exception e) {
            LOGGER.error("Failed to build allure report for {}", chain.getName(), e);
            allureBuildResult(false, stackTraceToString(e)).dumpToCustomData(customBuildData);
        } finally {
            deleteQuietly(artifactsTempDir);
            deleteQuietly(allureReportDir);
        }
    }


}
