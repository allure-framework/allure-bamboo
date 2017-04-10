package io.qameta.allure.bamboo;

import com.atlassian.bamboo.build.BuildDefinition;
import com.atlassian.bamboo.chains.Chain;
import com.atlassian.bamboo.chains.ChainExecution;
import com.atlassian.bamboo.chains.ChainResultsSummary;
import com.atlassian.bamboo.chains.plugins.PostChainAction;
import com.atlassian.bamboo.v2.build.BaseConfigurablePlugin;
import com.google.common.io.Files;
import io.qameta.allure.bamboo.config.AllureBuildConfig;
import io.qameta.allure.bamboo.config.AllureGlobalConfig;
import io.qameta.allure.bamboo.info.AllureBuildResult;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

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
    public void execute(@NotNull Chain chain, @NotNull ChainResultsSummary chainResultsSummary, @NotNull ChainExecution chainExecution) throws InterruptedException, Exception {
        LOGGER.info("Build has completed");

        final BuildDefinition buildDef = chain.getBuildDefinition();
        final AllureGlobalConfig globalConfig = settingsManager.getSettings();
        final AllureBuildConfig buildConfig = AllureBuildConfig.fromContext(buildDef.getCustomConfiguration());
        final boolean allureEnabled = buildConfig.isEnabled() || (!buildConfig.isEnabledSet() && globalConfig.isEnabledByDefault());
        final boolean isEnabledForFailedOnly = buildConfig.isOnlyForFailed();
        if (!allureEnabled || (isEnabledForFailedOnly && !chainResultsSummary.isFailed())) {
            return;
        }
        final File artifactsTempDir = Files.createTempDir();
        final File allureReportDir = Files.createTempDir();
        try {
            final String executable = ofNullable(buildConfig.getExecutable()).orElseGet(() -> {
                LOGGER.info("Allure executable has not been configured. Using default one!");
                return executablesManager.getDefaultAllureExecutable().orElseThrow(() ->
                        new RuntimeException("Could not find default Allure executable! Please configure plugin properly!"));
            });
            LOGGER.info("Allure Report was enabled for {}", chain.getName());
            LOGGER.info("Trying to get executable by name {} for {}", executable, chain.getName());
            allureExecutable.provide(globalConfig, executable).map(allure -> {
                LOGGER.info("Starting artifacts downloading into {} for {}", artifactsTempDir.getPath(), chain.getName());
                artifactsManager.downloadAllArtifactsTo(chainResultsSummary, artifactsTempDir);
                LOGGER.info("Starting allure generate into {} for {}", allureReportDir.getPath(), chain.getName());
                allure.generate(artifactsTempDir.toPath(), allureReportDir.toPath());
                LOGGER.info("Allure has been generated successfully for {}", chain.getName());
                artifactsManager.uploadReportArtifacts(chain, chainResultsSummary, allureReportDir)
                        .ifPresent(result -> result.toCustomData(chainResultsSummary.getCustomBuildData()));
                return true;
            }).orElseThrow(() -> new RuntimeException("Failed to find Allure executable by name " + executable));
        } catch (Exception e) {
            LOGGER.error("Failed to build allure report for {}", chain.getName(), e);
            final AllureBuildResult result = new AllureBuildResult(false);
            result.setFailureDetails(stackTraceToString(e));
            result.toCustomData(chainResultsSummary.getCustomBuildData());
        } finally {
            deleteQuietly(artifactsTempDir);
            deleteQuietly(allureReportDir);
        }
    }


}
