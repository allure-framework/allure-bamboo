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

import com.atlassian.bamboo.build.BuildDefinition;
import com.atlassian.bamboo.chains.ChainExecution;
import com.atlassian.bamboo.chains.ChainResultsSummary;
import com.atlassian.bamboo.chains.plugins.PostChainAction;
import com.atlassian.bamboo.configuration.AdministrationConfiguration;
import com.atlassian.bamboo.plan.cache.ImmutableChain;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.resultsummary.ResultsSummaryManager;
import com.atlassian.bamboo.v2.build.BaseConfigurablePlugin;
import com.atlassian.spring.container.ContainerManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.qameta.allure.bamboo.info.AddExecutorInfo;
import io.qameta.allure.bamboo.info.allurewidgets.summary.Summary;
import io.qameta.allure.bamboo.util.Downloader;
import io.qameta.allure.bamboo.util.FileStringReplacer;
import io.qameta.allure.bamboo.util.ZipUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import static io.qameta.allure.bamboo.AllureBuildResult.allureBuildResult;
import static io.qameta.allure.bamboo.util.ExceptionUtil.stackTraceToString;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

@SuppressWarnings("ConstantConditions")
public class AllureBuildCompleteAction extends BaseConfigurablePlugin implements PostChainAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureBuildCompleteAction.class);
    private static final String HISTORY_JSON = "history.json";
    private static final String HISTORY = "history";

    private static final List<String> HISTORY_FILES = Arrays.asList(HISTORY_JSON,
            "history-trend.json", "categories-trend.json", "duration-trend.json");


    private final AllureExecutableProvider allureExecutable;
    private final AllureSettingsManager settingsManager;
    private final AllureArtifactsManager artifactsManager;
    private final BambooExecutablesManager executablesManager;
    private final ResultsSummaryManager resultsSummaryManager;
    private final AdministrationConfiguration adminConfiguration;

    public AllureBuildCompleteAction(final AllureExecutableProvider allureExecutable,
                                     final AllureSettingsManager settingsManager,
                                     final AllureArtifactsManager artifactsManager,
                                     final BambooExecutablesManager executablesManager,
                                     final ResultsSummaryManager resultsSummaryManager) {
        this.allureExecutable = allureExecutable;
        this.settingsManager = settingsManager;
        this.artifactsManager = artifactsManager;
        this.executablesManager = executablesManager;
        this.resultsSummaryManager = resultsSummaryManager;
        this.adminConfiguration = (AdministrationConfiguration) ContainerManager
                .getComponent("administrationConfiguration");
    }

    @Override
    @SuppressWarnings({"ExecutableStatementCount", "PMD.NcssCount"})
    public void execute(final @NotNull ImmutableChain chain,
                        final @NotNull ChainResultsSummary chainResultsSummary,
                        final @NotNull ChainExecution chainExecution) {

        final BuildDefinition buildDef = chain.getBuildDefinition();
        final AllureGlobalConfig globalConfig = settingsManager.getSettings();
        final AllureBuildConfig buildConfig = AllureBuildConfig.fromContext(buildDef.getCustomConfiguration());
        final boolean allureEnabled = buildConfig.isEnabled()
                || !buildConfig.isEnabledSet() && globalConfig.isEnabledByDefault();
        final boolean isEnabledForFailedOnly = buildConfig.isOnlyForFailed();
        if (!allureEnabled || isEnabledForFailedOnly && !chainResultsSummary.isFailed()) {
            return;
        }

        try {

            final Path artifactsTempDir = Files.createTempDirectory("tmp_artifact");
            final Path allureReportDir = Files.createTempDirectory("tmp_report");

            buildReport(artifactsTempDir,
                    allureReportDir,
                    chainResultsSummary,
                    chain,
                    buildConfig,
                    globalConfig,
                    chainExecution);

            FileUtils.deleteQuietly(artifactsTempDir.toFile());
            FileUtils.deleteQuietly(allureReportDir.toFile());
        } catch (Exception e) {
            LOGGER.error("Failed to create tmp folders to build report.", e);
        }
    }

    private void buildReport(final Path artifactsTempDir,
                             final Path allureReportDir,
                             final ChainResultsSummary chainResultsSummary,
                             final ImmutableChain chain,
                             final AllureBuildConfig buildConfig,
                             final AllureGlobalConfig globalConfig,
                             final ChainExecution chainExecution) {

        final Map<String, String> customBuildData = chainResultsSummary.getCustomBuildData();
        try {
            final String executable = Optional.ofNullable(buildConfig.getExecutable())
                    .orElse(executablesManager.getDefaultAllureExecutable()
                            .orElseThrow(() -> new RuntimeException("Could not find default Allure executable!"
                                    + " Please configure plugin properly!")));

            LOGGER.info("Allure Report is enabled for {}", chain.getName());
            LOGGER.info("Trying to get executable by name {} for {}", executable, chain.getName());

            AllureExecutable allure = allureExecutable.provide(globalConfig, executable)
                    .orElseThrow(() -> new RuntimeException("Failed to find Allure executable by name " + executable));

            // Creating a copy for customize report
            final Path copyPath = Files.createTempDirectory("tmp_cmd");
            if (globalConfig.isCustomLogoEnabled()) {
                allure = allure.getTempCopy(copyPath);
            }

            LOGGER.info("Starting artifacts downloading into {} for {}", artifactsTempDir, chain.getName());
            final Collection<Path> artifactsPaths = artifactsManager.downloadAllArtifactsTo(
                    chainResultsSummary, artifactsTempDir.toFile(), buildConfig.getArtifactName());
            if (artifactsTempDir.toFile().list().length == 0) {
                allureBuildResult(false, "Build result does not have any uploaded artifacts!")
                        .dumpToCustomData(customBuildData);
            } else {
                LOGGER.info("Starting allure generate into {} for {}", allureReportDir, chain.getName());
                prepareResults(artifactsPaths.stream().map(Path::toFile).collect(toList()), chain, chainExecution);

                // Setting the new logo in the allure libraries before generate the report.
                if (globalConfig.isCustomLogoEnabled()) {
                    allure.setCustomLogo(buildConfig.getCustomLogoUrl());
                }
                allure.generate(artifactsPaths, allureReportDir);
                // Setting report name
                this.finalizeReport(allureReportDir,
                        chainExecution.getPlanResultKey().getBuildNumber(), chain.getBuildName());

                // Create an exportable zip with the report
                ZipUtil.zipReportFolder(allureReportDir, allureReportDir.resolve("report.zip"));

                LOGGER.info("Allure has been generated successfully for {}", chain.getName());
                artifactsManager.uploadReportArtifacts(chain, chainResultsSummary, allureReportDir.toFile())
                        .ifPresent(result -> result.dumpToCustomData(customBuildData));
            }
            FileUtils.deleteQuietly(copyPath.toFile());
            if (globalConfig.isEnabledReportsCleanup()
                    && buildConfig.getMaxStoredReportsCount() != null
                    && buildConfig.getMaxStoredReportsCount() > 0) {
                artifactsManager.cleanupOldReportArtifacts(chain, buildConfig.getMaxStoredReportsCount());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to build allure report for {}", chain.getName(), e);
            allureBuildResult(false, stackTraceToString(e)).dumpToCustomData(customBuildData);
        }
    }

    private void finalizeReport(final @NotNull Path allureReportDir,
                                final int buildNumber,
                                final String buildName) throws IOException {

        // Update Report Name (It is the way now)
        final Path widgetsJsonPath = allureReportDir
                .resolve("widgets")
                .resolve("summary.json");
        final ObjectMapper mapper = new JsonMapper();
        final Summary summary = mapper.readValue(widgetsJsonPath.toFile(), Summary.class);
        summary.setReportName(format("Build %s - %s", buildNumber, buildName));
        mapper.writeValue(widgetsJsonPath.toFile(), summary);

        // Deleting title from Logo
        final Path appJsPath = allureReportDir.resolve("app.js");
        FileStringReplacer.replaceInFile(appJsPath,
                Pattern.compile(">Allure</span>",
                        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.COMMENTS),
                ">&nbsp;</span>"
        );

        // Changing page title
        final Path indexHtmlPath = allureReportDir.resolve("index.html");
        FileStringReplacer.replaceInFile(indexHtmlPath,
                Pattern.compile("<title>.*</title>",
                        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.COMMENTS),
                format("<title> Build %s - %s </title>", buildNumber, buildName)
        );
    }

    private void prepareResults(final List<File> artifactsTempDirs,
                                final @NotNull ImmutableChain chain,
                                final @NotNull ChainExecution chainExecution) {
        copyHistory(artifactsTempDirs, chain.getPlanKey().getKey(), chainExecution.getPlanResultKey().getBuildNumber());
        addExecutorInfo(artifactsTempDirs, chain, chainExecution.getPlanResultKey().getBuildNumber());
    }

    /**
     * Write the history file to results directory.
     */
    private void copyHistory(final @NotNull List<File> artifactsTempDirs,
                             final String planKey,
                             final int buildNumber) {
        try {
            final Path tmpDirToDownloadHistory = Files.createTempDirectory("tmp_history");
            getLastBuildNumberWithHistory(planKey, buildNumber)
                    .ifPresent(buildId -> copyHistoryFiles(planKey, tmpDirToDownloadHistory, buildId));
            artifactsTempDirs.forEach(artifactsTempDir -> {
                try {
                    FileUtils.copyDirectory(tmpDirToDownloadHistory.toFile(),
                            artifactsTempDir.toPath().resolve(HISTORY).toFile());
                } catch (IOException e) {
                    LOGGER.error("Failed to copy history files from temp directory into artifacts directory", e);
                }
            });
            FileUtils.deleteQuietly(tmpDirToDownloadHistory.toFile());
        } catch (Exception e) {
            LOGGER.error("Failed to create tmp history folder", e);
        }
    }

    private void copyHistoryFiles(final String planKey,
                                  final Path historyDir,
                                  final Integer buildNumber) {
        HISTORY_FILES.forEach(historyFile ->
                copyArtifactToHistoryFolder(historyDir, historyFile, planKey, buildNumber)
        );
    }

    private Optional<Integer> getLastBuildNumberWithHistory(final String planKey,
                                                            final int buildNumber) {
        int currentBuild = buildNumber;
        do {
            final ResultsSummary lastBuild = resultsSummaryManager.findLastBuildResultBefore(planKey, currentBuild);
            if (Objects.isNull(lastBuild)) {
                return Optional.empty();
            }
            currentBuild = lastBuild.getBuildNumber();
        } while (!historyArtifactExists(planKey, currentBuild));
        return Optional.of(currentBuild);
    }

    private boolean historyArtifactExists(final String planKey,
                                          final int buildId) {
        final String artifactUrl = getHistoryArtifactUrl(HISTORY_JSON, planKey, buildId);
        final ObjectMapper mapper = new JsonMapper();

        try {
            final Path historyTmpFile = Files.createTempFile(HISTORY, ".json");
            Downloader.download(new URL(artifactUrl), historyTmpFile);
            mapper.readValue(historyTmpFile.toFile(), Object.class);
            FileUtils.deleteQuietly(historyTmpFile.toFile());
            return true;
        } catch (Exception e) {
            LOGGER.info("Cannot connect to artifact or the artifact is not valid {}.", artifactUrl, e);
            return false;
        }
    }

    private void copyArtifactToHistoryFolder(final Path historyFolder,
                                             final String fileName,
                                             final String planKey,
                                             final int buildId) {
        try (InputStream inputStream = getArtifactContent(fileName, planKey, buildId)) {
            Files.createDirectories(historyFolder);
            Files.copy(inputStream, historyFolder.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("Could not copy file {}", fileName);
        }
    }

    private InputStream getArtifactContent(final String fileName,
                                           final String planKey,
                                           final int buildId) throws IOException {
        final URL artifactUrl = new URL(getHistoryArtifactUrl(fileName, planKey, buildId));
        final URLConnection uc = artifactUrl.openConnection();
        return uc.getInputStream();
    }

    @NotNull
    private String getHistoryArtifactUrl(final String fileName,
                                         final String planKey,
                                         final int buildId) {
        return format("%s/plugins/servlet/allure/report/%s/%s/history/%s",
                getBambooBaseUrl(), planKey, buildId, fileName);
    }

    private void addExecutorInfo(final @NotNull List<File> artifactsTempDirs,
                                 final @NotNull ImmutableChain chain,
                                 final int buildNumber) {
        final String rootUrl = getBambooBaseUrl();
        final String buildName = chain.getBuildName();
        final String buildUrl = format("%s/browse/%s-%s", rootUrl, chain.getPlanKey().getKey(), buildNumber);
        final String reportUrl = format("%s/plugins/servlet/allure/report/%s/%s/", rootUrl,
                chain.getPlanKey().getKey(), buildNumber);
        final AddExecutorInfo executorInfo = new AddExecutorInfo(
                rootUrl, Integer.toString(buildNumber), buildName, buildUrl, reportUrl);
        artifactsTempDirs.forEach(executorInfo::invoke);
    }


    /**
     * Returns the base url of bamboo server.
     */
    @NotNull
    private String getBambooBaseUrl() {
        if (this.adminConfiguration != null) {
            return StringUtils.isNoneBlank(this.adminConfiguration.getBaseUrl())
                    ? this.adminConfiguration.getBaseUrl() : "";
        }
        return "";
    }
}
