/*
 *  Copyright 2016-2023 Qameta Software OÜ
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
import io.qameta.allure.bamboo.info.AddExecutorInfo;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
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

import static com.google.common.io.Files.createTempDir;
import static io.qameta.allure.bamboo.AllureBuildResult.allureBuildResult;
import static io.qameta.allure.bamboo.util.ExceptionUtil.stackTraceToString;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.codehaus.plexus.util.FileUtils.copyDirectory;

@SuppressWarnings("ConstantConditions")
public class AllureBuildCompleteAction extends BaseConfigurablePlugin implements PostChainAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureBuildCompleteAction.class);
    private static final String HISTORY_JSON = "history.json";
    private static final List<String> HISTORY_FILES = Arrays.asList(HISTORY_JSON, "history-trend.json");

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

    @SuppressWarnings("PMD.NcssCount")
    @Override
    public void execute(final @NotNull ImmutableChain chain,
                        final @NotNull ChainResultsSummary chainResultsSummary,
                        final @NotNull ChainExecution chainExecution) throws Exception {
        final BuildDefinition buildDef = chain.getBuildDefinition();
        final AllureGlobalConfig globalConfig = settingsManager.getSettings();
        final AllureBuildConfig buildConfig = AllureBuildConfig.fromContext(buildDef.getCustomConfiguration());
        final boolean allureEnabled = buildConfig.isEnabled()
                || !buildConfig.isEnabledSet() && globalConfig.isEnabledByDefault();
        final boolean isEnabledForFailedOnly = buildConfig.isOnlyForFailed();
        if (!allureEnabled || isEnabledForFailedOnly && !chainResultsSummary.isFailed()) {
            return;
        }
        final File artifactsTempDir = createTempDir();
        final File allureReportDir = new File(createTempDir(), "report");
        final Map<String, String> customBuildData = chainResultsSummary.getCustomBuildData();
        try {
            final String executable = Optional.ofNullable(buildConfig.getExecutable())
                    .orElse(executablesManager.getDefaultAllureExecutable()
                            .orElseThrow(() -> new RuntimeException("Could not find default Allure executable!"
                                    + " Please configure plugin properly!")));

            LOGGER.info("Allure Report is enabled for {}", chain.getName());
            LOGGER.info("Trying to get executable by name {} for {}", executable, chain.getName());

            final AllureExecutable allure = allureExecutable.provide(globalConfig, executable).orElseThrow(
                    () -> new RuntimeException("Failed to find Allure executable by name " + executable)
            );

            LOGGER.info("Starting artifacts downloading into {} for {}", artifactsTempDir, chain.getName());
            final Collection<Path> artifactsPaths = artifactsManager.downloadAllArtifactsTo(
                    chainResultsSummary, artifactsTempDir, buildConfig.getArtifactName());
            if (artifactsTempDir.list().length == 0) {
                allureBuildResult(false, "Build result does not have any uploaded artifacts!")
                        .dumpToCustomData(customBuildData);
            } else {
                LOGGER.info("Starting allure generate into {} for {}", allureReportDir.getPath(), chain.getName());
                prepareResults(artifactsPaths.stream().map(Path::toFile).collect(toList()), chain, chainExecution);
                allure.generate(artifactsPaths, allureReportDir.toPath());
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

    private void prepareResults(final List<File> artifactsTempDirs,
                                final ImmutableChain chain,
                                final ChainExecution chainExecution) throws Exception {
        copyHistory(artifactsTempDirs, chain.getPlanKey().getKey(), chainExecution.getPlanResultKey().getBuildNumber());
        addExecutorInfo(artifactsTempDirs, chain, chainExecution.getPlanResultKey().getBuildNumber());
    }

    /**
     * Write the history file to results directory.
     */
    private void copyHistory(final List<File> artifactsTempDirs,
                             final String planKey,
                             final int buildNumber) throws Exception {
        final Path tmpDirToDownloadHistory = createTempDir().toPath();
        getLastBuildNumberWithHistory(planKey, buildNumber)
                .ifPresent(buildId -> copyHistoryFiles(planKey, tmpDirToDownloadHistory, buildId));
        artifactsTempDirs.forEach(artifactsTempDir -> {
            try {
                copyDirectory(tmpDirToDownloadHistory.toFile(), artifactsTempDir.toPath().resolve("history").toFile());
            } catch (IOException e) {
                LOGGER.error("Failed to copy history files from temp directory into artifacts directory", e);
            }
        });
        deleteQuietly(tmpDirToDownloadHistory.toFile());
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
        try {
            HttpURLConnection.setFollowRedirects(false);
            final HttpURLConnection con = (HttpURLConnection) new URL(artifactUrl).openConnection();
            con.setRequestMethod("HEAD");
            return con.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            LOGGER.info("Cannot connect to artifact {}.", artifactUrl, e);
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

    private void addExecutorInfo(final List<File> artifactsTempDirs,
                                 final ImmutableChain chain,
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
                    ? this.adminConfiguration.getBaseUrl()
                    : "";
        }
        return "";
    }
}
