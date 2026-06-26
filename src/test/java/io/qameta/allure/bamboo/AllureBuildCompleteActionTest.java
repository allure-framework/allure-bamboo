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
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.chains.ChainExecution;
import com.atlassian.bamboo.chains.ChainResultsSummary;
import com.atlassian.bamboo.configuration.AdministrationConfiguration;
import com.atlassian.bamboo.plan.cache.ImmutableChain;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.resultsummary.ResultsSummaryManager;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.sun.net.httpserver.HttpServer;
import io.qameta.allure.bamboo.info.allurewidgets.summary.Summary;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.atlassian.bamboo.plan.PlanKeys.getPlanKey;
import static com.atlassian.bamboo.plan.PlanKeys.getPlanResultKey;
import static io.qameta.allure.bamboo.AllureBuildResult.allureBuildResult;
import static io.qameta.allure.bamboo.AllureBuildResult.fromCustomData;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_ENABLED;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_EXECUTABLE;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_FAILED_ONLY;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_MAX_STORED_REPORTS_COUNT;
import static io.qameta.allure.bamboo.TestSupport.attachDirectoryTree;
import static io.qameta.allure.bamboo.TestSupport.attachText;
import static io.qameta.allure.bamboo.TestSupport.step;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.junit.MockitoJUnit.rule;

@SuppressWarnings("checkstyle:MultipleStringLiterals")
public class AllureBuildCompleteActionTest {

    private static final String PLAN_KEY = "PROJ-PLAN";
    private static final int BUILD_NUMBER = 5;

    @Rule
    public MockitoRule mockitoRule = rule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private AllureExecutableProvider executableProvider;
    @Mock
    private AllureSettingsManager settingsManager;
    @Mock
    private AllureArtifactsManager artifactsManager;
    @Mock
    private BambooExecutablesManager executablesManager;
    @Mock
    private ResultsSummaryManager resultsSummaryManager;
    @Mock
    private AllureExecutable allureExecutable;
    @Mock
    private AdministrationConfiguration administrationConfiguration;
    @Mock
    private ImmutableChain chain;
    @Mock
    private BuildDefinition buildDefinition;
    @Mock
    private ChainResultsSummary chainResultsSummary;
    @Mock
    private ChainExecution chainExecution;
    @Mock
    private BuildLogger buildLogger;

    private HttpServer historyServer;

    @Before
    public void setUp() {
        when(chain.getBuildDefinition()).thenReturn(buildDefinition);
        when(chain.getName()).thenReturn("Smoke Chain");
        when(chain.getBuildName()).thenReturn("Smoke Build");
        when(chain.getPlanKey()).thenReturn(getPlanKey(PLAN_KEY));
        when(chain.getBuildLogger()).thenReturn(buildLogger);
        when(chainResultsSummary.getCustomBuildData()).thenReturn(new HashMap<>());
        when(chainResultsSummary.getBuildNumber()).thenReturn(BUILD_NUMBER);
        when(chainExecution.getPlanResultKey()).thenReturn(getPlanResultKey(PLAN_KEY, BUILD_NUMBER));
        when(executablesManager.getDefaultAllureExecutable()).thenReturn(Optional.of("exec-1"));
        when(resultsSummaryManager.findLastBuildResultBefore(anyString(), anyInt())).thenReturn(null);
    }

    @After
    public void tearDown() {
        if (historyServer != null) {
            historyServer.stop(0);
        }
    }

    @Test
    public void itShouldSkipExecutionWhenAllureIsDisabled() throws Exception {
        final Map<String, String> config = new HashMap<>();
        config.put(ALLURE_CONFIG_ENABLED, "false");
        when(buildDefinition.getCustomConfiguration()).thenReturn(config);
        when(settingsManager.getSettings()).thenReturn(new AllureGlobalConfig("true",
                "false",
                "https://downloads.example/",
                temporaryFolder.getRoot().getAbsolutePath(),
                "https://repo.example/",
                "false",
                "false"));

        newAction().execute(chain, chainResultsSummary, chainExecution);

        verify(artifactsManager, never()).downloadAllArtifactsTo(any(), any(File.class), anyString());
    }

    @Test
    public void itShouldSkipSuccessfulBuildsWhenConfiguredForFailedOnly() throws Exception {
        step("configure a successful chain that should skip Allure when failed-only mode is enabled", () -> {
            when(chainResultsSummary.isFailed()).thenReturn(false);
            when(buildDefinition.getCustomConfiguration()).thenReturn(config("exec-1", true, true, null));
            when(settingsManager.getSettings()).thenReturn(globalConfig(false, false));
            attachText("Build configuration", buildDefinition.getCustomConfiguration().toString());
        });

        step("execute the build completion action",
                () -> newAction().execute(chain, chainResultsSummary, chainExecution));

        step("verify report generation is skipped for a successful build", () -> {
            verify(artifactsManager, never()).downloadAllArtifactsTo(any(), any(File.class), anyString());
            verify(allureExecutable, never()).generate(any(Collection.class), any(Path.class));
        });
    }

    @Test
    public void itShouldRecordFailureWhenNoExecutableCanBeResolved() throws Exception {
        when(chainResultsSummary.isFailed()).thenReturn(true);
        when(buildDefinition.getCustomConfiguration()).thenReturn(config(null, true, false, null));
        when(settingsManager.getSettings()).thenReturn(globalConfig(false, false));
        when(executablesManager.getDefaultAllureExecutable()).thenReturn(Optional.empty());

        newAction().execute(chain, chainResultsSummary, chainExecution);

        final AllureBuildResult buildResult = fromCustomData(chainResultsSummary.getCustomBuildData());
        assertThat(buildResult.isSuccess()).isFalse();
        assertThat(buildResult.getFailureDetails()).contains("Could not find default Allure executable");
        verify(artifactsManager, never()).downloadAllArtifactsTo(any(), any(File.class), anyString());
    }

    @Test
    public void itShouldRecordFailureWhenNoArtifactsAreDownloaded() throws Exception {
        step("configure a failing chain whose Allure artifacts download to an empty set", () -> {
            when(chainResultsSummary.isFailed()).thenReturn(true);
            when(buildDefinition.getCustomConfiguration()).thenReturn(config("exec-1", true, false, null));
            when(settingsManager.getSettings()).thenReturn(globalConfig(false, false));
            when(executableProvider.provide(any(AllureGlobalConfig.class), anyString()))
                    .thenReturn(Optional.of(allureExecutable));
            when(artifactsManager.downloadAllArtifactsTo(any(ChainResultsSummary.class), any(File.class), anyString()))
                    .thenReturn(emptyList());
        });

        step("execute the build completion action",
                () -> newAction().execute(chain, chainResultsSummary, chainExecution));

        step("verify the action records a missing-artifacts failure without generating a report", () -> {
            final AllureBuildResult buildResult = fromCustomData(chainResultsSummary.getCustomBuildData());
            attachText("Failure details", buildResult.getFailureDetails());
            assertThat(buildResult.isSuccess()).isFalse();
            assertThat(buildResult.getFailureDetails()).contains("does not have any uploaded artifacts");
            verify(allureExecutable, never()).generate(any(Collection.class), any(Path.class));
        });
    }

    @Test
    public void itShouldGenerateUploadAndCleanupReport() throws Exception {
        step("configure report generation, upload, and cleanup for a failed chain", () -> {
            when(chainResultsSummary.isFailed()).thenReturn(true);
            when(buildDefinition.getCustomConfiguration()).thenReturn(config("exec-1", true, false, "2"));
            when(settingsManager.getSettings()).thenReturn(globalConfig(false, true));
            when(executableProvider.provide(any(AllureGlobalConfig.class), anyString()))
                    .thenReturn(Optional.of(allureExecutable));
            when(artifactsManager.downloadAllArtifactsTo(any(ChainResultsSummary.class), any(File.class), anyString()))
                    .thenAnswer(invocation -> {
                        final File baseDir = invocation.getArgument(1);
                        final Path artifactsDir = baseDir.toPath().resolve("artifacts");
                        Files.createDirectories(artifactsDir);
                        return new ArrayList<>(java.util.List.of(artifactsDir));
                    });
            when(allureExecutable.generate(any(Collection.class), any(Path.class))).thenAnswer(invocation -> {
                final Path reportDir = invocation.getArgument(1);
                TestSupport.writeMinimalReport(reportDir);
                attachDirectoryTree("Generated report before upload", reportDir);
                return new AllureGenerateResult("ok", true);
            });
            when(artifactsManager.uploadReportArtifacts(any(ImmutableChain.class),
                    any(ChainResultsSummary.class), any(File.class))).thenAnswer(invocation -> {
                final Path reportDir = ((File) invocation.getArgument(2)).toPath();
                final Summary summary = new JsonMapper()
                        .readValue(reportDir.resolve("widgets").resolve("summary.json").toFile(), Summary.class);
                attachDirectoryTree("Uploaded report contents", reportDir);
                assertThat(summary.getReportName()).isEqualTo("Build 5 - Smoke Build");
                assertThat(Files.readString(reportDir.resolve("app.js"))).contains(">&nbsp;</span>");
                assertThat(Files.readString(reportDir.resolve("index.html")))
                        .contains("<title> Build 5 - Smoke Build </title>");
                assertThat(reportDir.resolve("report.zip")).exists();
                return Optional.of(allureBuildResult(true, null).withHandlerClass("handler"));
            });
        });

        step("execute the build completion action",
                () -> newAction().execute(chain, chainResultsSummary, chainExecution));

        step("verify the successful result stores the handler and triggers cleanup", () -> {
            final AllureBuildResult buildResult = fromCustomData(chainResultsSummary.getCustomBuildData());
            attachText("Artifact handler class", buildResult.getArtifactHandlerClass());
            assertThat(buildResult.isSuccess()).isTrue();
            assertThat(buildResult.getArtifactHandlerClass()).isEqualTo("handler");
            verify(artifactsManager).cleanupOldReportArtifacts(chain, 2);
        });
    }

    @Test
    public void itShouldCopyHistoryFromThePreviousBuild() throws Exception {
        historyServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        createHistoryContext("history.json", "[]");
        createHistoryContext("history-trend.json", "[]");
        createHistoryContext("categories-trend.json", "[]");
        createHistoryContext("duration-trend.json", "[]");
        historyServer.start();

        when(administrationConfiguration.getBaseUrl())
                .thenReturn(String.format("http://127.0.0.1:%s", historyServer.getAddress().getPort()));
        when(chainResultsSummary.isFailed()).thenReturn(true);
        when(buildDefinition.getCustomConfiguration()).thenReturn(config("exec-1", true, false, null));
        when(settingsManager.getSettings()).thenReturn(globalConfig(false, false));
        when(executableProvider.provide(any(AllureGlobalConfig.class), anyString()))
                .thenReturn(Optional.of(allureExecutable));
        when(artifactsManager.downloadAllArtifactsTo(any(ChainResultsSummary.class), any(File.class), anyString()))
                .thenAnswer(invocation -> {
                    final File baseDir = invocation.getArgument(1);
                    final Path artifactsDir = baseDir.toPath().resolve("artifacts");
                    Files.createDirectories(artifactsDir);
                    return new ArrayList<>(java.util.List.of(artifactsDir));
                });
        final ResultsSummary previousResult = org.mockito.Mockito.mock(ResultsSummary.class);
        when(previousResult.getBuildNumber()).thenReturn(4);
        when(resultsSummaryManager.findLastBuildResultBefore(PLAN_KEY, BUILD_NUMBER)).thenReturn(previousResult);
        when(allureExecutable.generate(any(Collection.class), any(Path.class))).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            final Collection<Path> sourceDirs = invocation.getArgument(0);
            final Path artifactsDir = sourceDirs.iterator().next();
            assertThat(artifactsDir.resolve("history").resolve("history.json")).exists();
            assertThat(artifactsDir.resolve("history").resolve("history-trend.json")).exists();
            TestSupport.writeMinimalReport(invocation.getArgument(1));
            return new AllureGenerateResult("ok", true);
        });
        when(artifactsManager.uploadReportArtifacts(any(ImmutableChain.class),
                any(ChainResultsSummary.class), any(File.class)))
                .thenReturn(Optional.of(allureBuildResult(true, null).withHandlerClass("handler")));

        newAction().execute(chain, chainResultsSummary, chainExecution);

        final AllureBuildResult buildResult = fromCustomData(chainResultsSummary.getCustomBuildData());
        assertThat(buildResult.isSuccess()).isTrue();
    }

    private void createHistoryContext(final String fileName,
                                      final String body) {
        historyServer.createContext("/plugins/servlet/allure/report/" + PLAN_KEY + "/4/history/" + fileName,
                exchange -> {
                    final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, bytes.length);
                    exchange.getResponseBody().write(bytes);
                    exchange.close();
                });
    }

    private Map<String, String> config(final String executable,
                                       final boolean enabled,
                                       final boolean failedOnly,
                                       final String maxStoredReportsCount) {
        final Map<String, String> config = new HashMap<>();
        config.put(ALLURE_CONFIG_ENABLED, Boolean.toString(enabled));
        config.put(ALLURE_CONFIG_FAILED_ONLY, Boolean.toString(failedOnly));
        if (executable != null) {
            config.put(ALLURE_CONFIG_EXECUTABLE, executable);
        }
        if (maxStoredReportsCount != null) {
            config.put(ALLURE_CONFIG_MAX_STORED_REPORTS_COUNT, maxStoredReportsCount);
        }
        return config;
    }

    private AllureGlobalConfig globalConfig(final boolean customLogoEnabled,
                                            final boolean cleanupEnabled) {
        return new AllureGlobalConfig("true",
                "false",
                "https://downloads.example/",
                temporaryFolder.getRoot().getAbsolutePath(),
                "https://repo.example/",
                Boolean.toString(customLogoEnabled),
                Boolean.toString(cleanupEnabled));
    }

    private AllureBuildCompleteAction newAction() {
        return new AllureBuildCompleteAction(executableProvider,
                settingsManager,
                artifactsManager,
                executablesManager,
                resultsSummaryManager,
                administrationConfiguration);
    }
}
