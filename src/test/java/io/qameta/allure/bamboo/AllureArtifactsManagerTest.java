/*
 *  Copyright 2016-2026 Qameta Software Inc
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

import com.atlassian.bamboo.artifact.MutableArtifact;
import com.atlassian.bamboo.build.BuildDefinition;
import com.atlassian.bamboo.build.BuildDefinitionManager;
import com.atlassian.bamboo.build.artifact.AgentLocalArtifactHandler;
import com.atlassian.bamboo.build.artifact.ArtifactFileData;
import com.atlassian.bamboo.build.artifact.ArtifactHandler;
import com.atlassian.bamboo.build.artifact.ArtifactHandlerPublishingResult;
import com.atlassian.bamboo.build.artifact.ArtifactLink;
import com.atlassian.bamboo.build.artifact.ArtifactLinkDataProvider;
import com.atlassian.bamboo.build.artifact.ArtifactLinkManager;
import com.atlassian.bamboo.build.artifact.BambooRemoteArtifactHandler;
import com.atlassian.bamboo.build.artifact.FileSystemArtifactLinkDataProvider;
import com.atlassian.bamboo.build.artifact.TrampolineArtifactFileData;
import com.atlassian.bamboo.build.artifact.handlers.ArtifactHandlersService;
import com.atlassian.bamboo.chains.ChainResultsSummary;
import com.atlassian.bamboo.chains.ChainStageResult;
import com.atlassian.bamboo.plan.cache.ImmutableChain;
import com.atlassian.bamboo.plugin.descriptor.ArtifactHandlerModuleDescriptor;
import com.atlassian.bamboo.resultsummary.BuildResultsSummary;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.resultsummary.ResultsSummaryManager;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.atlassian.bamboo.plan.PlanKeys.getPlanKey;
import static com.atlassian.bamboo.plan.PlanKeys.getPlanResultKey;
import static io.qameta.allure.bamboo.AllureBuildResult.allureBuildResult;
import static io.qameta.allure.bamboo.TestSupport.attachDirectoryTree;
import static io.qameta.allure.bamboo.TestSupport.attachText;
import static io.qameta.allure.bamboo.TestSupport.step;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.junit.MockitoJUnit.rule;

@SuppressWarnings("checkstyle:MultipleStringLiterals")
public class AllureArtifactsManagerTest {

    private static final String PLAN_KEY = "PROJ-PLAN";
    private static final String BUILD_NUMBER = "5";

    @Rule
    public MockitoRule mockitoRule = rule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private PluginAccessor pluginAccessor;
    @Mock
    private ArtifactHandlersService artifactHandlersService;
    @Mock
    private BuildDefinitionManager buildDefinitionManager;
    @Mock
    private ResultsSummaryManager resultsSummaryManager;
    @Mock
    private ArtifactLinkManager artifactLinkManager;
    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private AllureSettingsManager settingsManager;
    @Mock
    private BuildDefinition buildDefinition;

    private AllureArtifactsManager manager;
    private Path localStorage;

    @Before
    public void setUp() throws Exception {
        localStorage = temporaryFolder.newFolder("allure-storage").toPath();
        when(settingsManager.getSettings()).thenReturn(
                new AllureGlobalConfig(
                        "true",
                        "false",
                        "https://downloads.example/",
                        localStorage.toString(),
                        "false",
                        "false"
                )
        );
        when(buildDefinitionManager.getBuildDefinition(getPlanKey(PLAN_KEY))).thenReturn(buildDefinition);
        when(buildDefinition.getCustomConfiguration()).thenReturn(new HashMap<>());
        when(artifactHandlersService.getRuntimeConfiguration()).thenReturn(new HashMap<>());
        when(applicationProperties.getBaseUrl(UrlMode.ABSOLUTE)).thenReturn("https://bamboo.example");
        manager = new AllureArtifactsManager(
                pluginAccessor,
                artifactHandlersService,
                buildDefinitionManager,
                resultsSummaryManager,
                artifactLinkManager,
                applicationProperties,
                settingsManager
        );
    }

    @Test
    public void itShouldResolveLocalArtifactUrls() throws Exception {
        final Path reportFile = localStorage.resolve("allure-reports").resolve(PLAN_KEY).resolve(BUILD_NUMBER)
                .resolve("index.html");
        final ResultsSummary resultsSummary = resultsSummaryWithHandler(BambooRemoteArtifactHandler.class.getName());
        Files.createDirectories(reportFile.getParent());
        Files.writeString(reportFile, "report", StandardCharsets.UTF_8);

        final BambooRemoteArtifactHandler handler = org.mockito.Mockito.mock(BambooRemoteArtifactHandler.class);
        doReturn(List.of(handler))
                .when(pluginAccessor)
                .getModules(org.mockito.ArgumentMatchers.any(java.util.function.Predicate.class));
        when(resultsSummaryManager.getResultsSummary(getPlanResultKey(PLAN_KEY, Integer.parseInt(BUILD_NUMBER))))
                .thenReturn(resultsSummary);

        final String artifactUrl = manager.getArtifactUrl(PLAN_KEY, BUILD_NUMBER, "index.html").orElse(null);

        assertThat(artifactUrl).isEqualTo(reportFile.toUri().toURL().toString());
    }

    @Test
    public void itShouldOpenLocalArtifactContentDirectly() throws Exception {
        final Path historyFile = localStorage.resolve("allure-reports").resolve(PLAN_KEY).resolve(BUILD_NUMBER)
                .resolve("history").resolve("history.json");
        final ResultsSummary resultsSummary = resultsSummaryWithHandler(BambooRemoteArtifactHandler.class.getName());
        Files.createDirectories(historyFile.getParent());
        Files.writeString(historyFile, "[{\"name\":\"trend\"}]", StandardCharsets.UTF_8);
        final BambooRemoteArtifactHandler handler = org.mockito.Mockito.mock(BambooRemoteArtifactHandler.class);
        doReturn(List.of(handler))
                .when(pluginAccessor)
                .getModules(org.mockito.ArgumentMatchers.any(java.util.function.Predicate.class));
        when(resultsSummaryManager.getResultsSummary(getPlanResultKey(PLAN_KEY, Integer.parseInt(BUILD_NUMBER))))
                .thenReturn(resultsSummary);

        final Optional<InputStream> content = manager.getArtifactInputStream(PLAN_KEY, BUILD_NUMBER, "history/history.json");

        assertThat(content).isPresent();
        try (InputStream in = content.get()) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).contains("trend");
        }
    }

    @Test
    public void itShouldReturnEmptyStreamWhenArtifactIsMissingOnDisk() throws Exception {
        final ResultsSummary resultsSummary = resultsSummaryWithHandler(BambooRemoteArtifactHandler.class.getName());
        final BambooRemoteArtifactHandler handler = org.mockito.Mockito.mock(BambooRemoteArtifactHandler.class);
        doReturn(List.of(handler))
                .when(pluginAccessor)
                .getModules(org.mockito.ArgumentMatchers.any(java.util.function.Predicate.class));
        when(resultsSummaryManager.getResultsSummary(getPlanResultKey(PLAN_KEY, Integer.parseInt(BUILD_NUMBER))))
                .thenReturn(resultsSummary);

        final Optional<InputStream> content = manager.getArtifactInputStream(PLAN_KEY, BUILD_NUMBER, "history/history.json");

        assertThat(content).isEmpty();
    }

    @Test
    public void itShouldRejectUnsafeLocalArtifactPaths() throws Exception {
        step("prepare a remote report summary with a local file lookup", () -> {
            final ResultsSummary resultsSummary = resultsSummaryWithHandler(BambooRemoteArtifactHandler.class.getName());
            final BambooRemoteArtifactHandler handler = org.mockito.Mockito.mock(BambooRemoteArtifactHandler.class);
            doReturn(List.of(handler))
                    .when(pluginAccessor)
                    .getModules(org.mockito.ArgumentMatchers.any(java.util.function.Predicate.class));
            when(resultsSummaryManager.getResultsSummary(getPlanResultKey(PLAN_KEY, Integer.parseInt(BUILD_NUMBER))))
                    .thenReturn(resultsSummary);
        });

        step("reject a path that escapes the report root", () -> {
            final AllurePluginException error = catchThrowableOfType(
                    () -> manager.getArtifactUrl(PLAN_KEY, BUILD_NUMBER, ".."),
                    AllurePluginException.class
            );
            assertThat(error).isNotNull();
            attachText("Rejected artifact path", ".. -> " + error.getMessage());
        });
    }

    @Test
    public void itShouldRejectMultiSegmentTraversalEscapingReportRoot() throws Exception {
        step("prepare a local (agent-handler) report summary", () -> {
            final ResultsSummary resultsSummary = resultsSummaryWithHandler(BambooRemoteArtifactHandler.class.getName());
            final BambooRemoteArtifactHandler handler = org.mockito.Mockito.mock(BambooRemoteArtifactHandler.class);
            doReturn(List.of(handler))
                    .when(pluginAccessor)
                    .getModules(org.mockito.ArgumentMatchers.any(java.util.function.Predicate.class));
            when(resultsSummaryManager.getResultsSummary(getPlanResultKey(PLAN_KEY, Integer.parseInt(BUILD_NUMBER))))
                    .thenReturn(resultsSummary);
        });

        step("reject a path whose final segment looks safe but which escapes the build directory", () -> {
            final String traversal = "../../../../etc/passwd";
            final AllurePluginException error = catchThrowableOfType(
                    () -> manager.getArtifactUrl(PLAN_KEY, BUILD_NUMBER, traversal),
                    AllurePluginException.class
            );
            attachText(
                    "Traversal attempt",
                    traversal + " -> " + (error == null ? "NOT REJECTED" : error.getMessage())
            );
            assertThat(error).isNotNull();
        });
    }

    @Test
    public void itShouldRejectTraversalForRemoteHandlerArtifacts() throws Exception {
        final ArtifactHandler handler = org.mockito.Mockito.mock(ArtifactHandler.class);
        final ArtifactLinkDataProvider dataProvider = org.mockito.Mockito.mock(ArtifactLinkDataProvider.class);
        final ResultsSummary resultsSummary = resultsSummaryWithHandler(ArtifactHandler.class.getName());
        doReturn(List.of(handler))
                .when(pluginAccessor)
                .getModules(org.mockito.ArgumentMatchers.any(java.util.function.Predicate.class));
        when(resultsSummaryManager.getResultsSummary(getPlanResultKey(PLAN_KEY, Integer.parseInt(BUILD_NUMBER))))
                .thenReturn(resultsSummary);
        when(handler.getArtifactLinkDataProvider(any(MutableArtifact.class), any())).thenReturn(dataProvider);

        final AllurePluginException error = catchThrowableOfType(
                () -> manager.getArtifactUrl(PLAN_KEY, BUILD_NUMBER, "../../../etc/passwd"),
                AllurePluginException.class
        );

        assertThat(error).isNotNull();
    }

    @Test
    public void itShouldRejectSymlinkEscapesFromLocalReports() throws Exception {
        final Path reportDir = localStorage.resolve("allure-reports").resolve(PLAN_KEY).resolve(BUILD_NUMBER);
        Files.createDirectories(reportDir);
        final Path outside = temporaryFolder.newFile("outside-secret.txt").toPath();
        Files.writeString(outside, "secret", StandardCharsets.UTF_8);
        try {
            Files.createSymbolicLink(reportDir.resolve("escape.html"), outside);
        } catch (Exception e) {
            org.junit.Assume.assumeNoException("Symlinks are not supported on this platform", e);
        }
        final ResultsSummary resultsSummary = resultsSummaryWithHandler(BambooRemoteArtifactHandler.class.getName());
        final BambooRemoteArtifactHandler handler = org.mockito.Mockito.mock(BambooRemoteArtifactHandler.class);
        doReturn(List.of(handler))
                .when(pluginAccessor)
                .getModules(org.mockito.ArgumentMatchers.any(java.util.function.Predicate.class));
        when(resultsSummaryManager.getResultsSummary(getPlanResultKey(PLAN_KEY, Integer.parseInt(BUILD_NUMBER))))
                .thenReturn(resultsSummary);

        final AllurePluginException error = catchThrowableOfType(
                () -> manager.getArtifactUrl(PLAN_KEY, BUILD_NUMBER, "escape.html"),
                AllurePluginException.class
        );

        assertThat(error).isNotNull();
    }

    @Test
    public void itShouldFallBackToIndexForRemoteDirectoryLikeArtifacts() {
        final ArtifactHandler handler = org.mockito.Mockito.mock(ArtifactHandler.class);
        final ArtifactLinkDataProvider dataProvider = org.mockito.Mockito.mock(ArtifactLinkDataProvider.class);
        final ArtifactFileData rootA = org.mockito.Mockito.mock(ArtifactFileData.class);
        final ArtifactFileData rootB = org.mockito.Mockito.mock(ArtifactFileData.class);
        final ArtifactFileData index = org.mockito.Mockito.mock(ArtifactFileData.class);
        final ResultsSummary resultsSummary = resultsSummaryWithHandler(ArtifactHandler.class.getName());
        doReturn(List.of(handler))
                .when(pluginAccessor)
                .getModules(org.mockito.ArgumentMatchers.any(java.util.function.Predicate.class));
        when(resultsSummaryManager.getResultsSummary(getPlanResultKey(PLAN_KEY, Integer.parseInt(BUILD_NUMBER))))
                .thenReturn(resultsSummary);
        when(handler.getArtifactLinkDataProvider(any(MutableArtifact.class), any()))
                .thenReturn(dataProvider);
        when(dataProvider.listObjects("")).thenReturn(List.of(rootA, rootB));
        when(dataProvider.listObjects("index.html")).thenReturn(List.of(index));
        when(index.getUrl()).thenReturn("https://reports.example/index.html");

        final String artifactUrl = manager.getArtifactUrl(PLAN_KEY, BUILD_NUMBER, "").orElse(null);

        assertThat(artifactUrl).isEqualTo("https://reports.example/index.html");
    }

    @Test
    public void itShouldDownloadFilesystemArtifactsRecursively() throws Exception {
        final FileSystemArtifactLinkDataProvider provider = org.mockito.Mockito
                .mock(FileSystemArtifactLinkDataProvider.class);
        final MutableArtifact artifact = org.mockito.Mockito.mock(MutableArtifact.class);
        final ArtifactLink link = org.mockito.Mockito.mock(ArtifactLink.class);
        final BuildResultsSummary buildResultsSummary = org.mockito.Mockito.mock(BuildResultsSummary.class);
        final ChainStageResult stageResult = org.mockito.Mockito.mock(ChainStageResult.class);
        final ChainResultsSummary chainResultsSummary = org.mockito.Mockito.mock(ChainResultsSummary.class);
        final File sourceDir = temporaryFolder.newFolder("source-artifacts");
        Files.writeString(sourceDir.toPath().resolve("result.json"), "{}", StandardCharsets.UTF_8);
        Files.createDirectories(sourceDir.toPath().resolve("history"));
        Files.writeString(sourceDir.toPath().resolve("history").resolve("history.json"), "[]", StandardCharsets.UTF_8);
        when(artifact.getLabel()).thenReturn("allure-results");
        when(link.getArtifact()).thenReturn(artifact);
        when(buildResultsSummary.getProducedArtifactLinks()).thenReturn(List.of(link));
        when(stageResult.getBuildResults()).thenReturn(java.util.Set.of(buildResultsSummary));
        when(chainResultsSummary.getStageResults()).thenReturn(List.of(stageResult));
        when(chainResultsSummary.getPlanKey()).thenReturn(getPlanKey(PLAN_KEY));
        when(chainResultsSummary.getBuildNumber()).thenReturn(Integer.parseInt(BUILD_NUMBER));
        when(artifactLinkManager.getArtifactLinkDataProvider(artifact)).thenReturn(provider);
        when(provider.getFile()).thenReturn(sourceDir);

        final Collection<Path> downloaded = manager.downloadAllArtifactsTo(
                chainResultsSummary, temporaryFolder.newFolder("downloaded"), null
        );

        final Path downloadedDir = downloaded.iterator().next();
        try (java.util.stream.Stream<Path> resultFiles = Files.walk(downloadedDir);
                java.util.stream.Stream<Path> historyFiles = Files.walk(downloadedDir)) {
            assertThat(resultFiles.anyMatch(path -> path.getFileName().toString().equals("result.json"))).isTrue();
            assertThat(historyFiles.anyMatch(path -> path.getFileName().toString().equals("history.json"))).isTrue();
        }
    }

    @Test
    public void itShouldDownloadRemoteArtifactsViaTrampolineLinks() throws Exception {
        final ArtifactLinkDataProvider provider = org.mockito.Mockito.mock(ArtifactLinkDataProvider.class);
        final MutableArtifact artifact = org.mockito.Mockito.mock(MutableArtifact.class);
        final ArtifactLink link = org.mockito.Mockito.mock(ArtifactLink.class);
        final BuildResultsSummary buildResultsSummary = org.mockito.Mockito.mock(BuildResultsSummary.class);
        final ChainStageResult stageResult = org.mockito.Mockito.mock(ChainStageResult.class);
        final ChainResultsSummary chainResultsSummary = org.mockito.Mockito.mock(ChainResultsSummary.class);
        final TrampolineArtifactFileData trampoline = org.mockito.Mockito.mock(TrampolineArtifactFileData.class);
        final ArtifactFileData delegate = org.mockito.Mockito.mock(ArtifactFileData.class);
        final Path sourceFile = temporaryFolder.newFile("remote-result.json").toPath();
        step("describe a trampoline artifact that points to a remote JSON result", () -> {
            Files.writeString(sourceFile, "{\"status\":\"passed\"}", StandardCharsets.UTF_8);
            when(artifact.getLabel()).thenReturn("allure-results");
            when(link.getArtifact()).thenReturn(artifact);
            when(buildResultsSummary.getProducedArtifactLinks()).thenReturn(List.of(link));
            when(stageResult.getBuildResults()).thenReturn(java.util.Set.of(buildResultsSummary));
            when(chainResultsSummary.getStageResults()).thenReturn(List.of(stageResult));
            when(chainResultsSummary.getPlanKey()).thenReturn(getPlanKey(PLAN_KEY));
            when(chainResultsSummary.getBuildNumber()).thenReturn(Integer.parseInt(BUILD_NUMBER));
            when(artifactLinkManager.getArtifactLinkDataProvider(artifact)).thenReturn(provider);
            when(provider.listObjects("")).thenReturn(List.of(trampoline));
            when(trampoline.getDelegate()).thenReturn(delegate);
            when(delegate.getFileType()).thenReturn(ArtifactFileData.FileType.REGULAR_FILE);
            when(delegate.getName()).thenReturn("nested/remote-result.json");
            when(delegate.getUrl()).thenReturn(sourceFile.toUri().toURL().toString());
            attachText("Remote artifact URL", sourceFile.toUri().toString());
        });

        final Collection<Path> downloaded = step(
                "download the remote artifact through the trampoline link",
                () -> manager.downloadAllArtifactsTo(
                        chainResultsSummary, temporaryFolder.newFolder("remote-download"), null
                )
        );

        final Path downloadedDir = downloaded.iterator().next();
        step("verify the flattened download contains the expected result file", () -> {
            attachDirectoryTree("Downloaded remote artifacts", downloadedDir);
            assertThat(downloadedDir.resolve("remote-result.json")).exists();
        });
    }

    @Test
    public void itShouldUploadReportsToLocalStorageForLocalHandlers() throws Exception {
        final BambooRemoteArtifactHandler handler = org.mockito.Mockito.mock(BambooRemoteArtifactHandler.class);
        final ImmutableChain chain = org.mockito.Mockito.mock(ImmutableChain.class);
        final ChainResultsSummary summary = org.mockito.Mockito.mock(ChainResultsSummary.class);
        final File reportDir = temporaryFolder.newFolder("report-local");
        step("prepare a local report directory and a compatible remote handler", () -> {
            Files.writeString(reportDir.toPath().resolve("index.html"), "<html/>", StandardCharsets.UTF_8);
            doReturn(List.of(handler))
                    .when(pluginAccessor)
                    .getModules(org.mockito.ArgumentMatchers.any(java.util.function.Predicate.class));
            when(chain.getBuildDefinition()).thenReturn(buildDefinition);
            when(chain.getPlanKey()).thenReturn(getPlanKey(PLAN_KEY));
            when(summary.getBuildNumber()).thenReturn(Integer.parseInt(BUILD_NUMBER));
            when(handler.canHandleArtifact(any(), any())).thenReturn(true);
        });

        final Optional<AllureBuildResult> result = step(
                "upload the report into local plugin storage",
                () -> manager.uploadReportArtifacts(chain, summary, reportDir)
        );

        final Path storedReport = localStorage.resolve("allure-reports").resolve(PLAN_KEY).resolve(BUILD_NUMBER)
                .resolve("index.html");
        step("verify the stored report is available under the build-specific directory", () -> {
            attachDirectoryTree("Stored local report", storedReport.getParent());
            assertThat(result).hasValueSatisfying(buildResult -> assertThat(buildResult.isSuccess()).isTrue());
            assertThat(storedReport).exists();
        });
    }

    @Test
    public void itShouldPublishReportsWithRemoteHandlers() throws Exception {
        final ArtifactHandler handler = org.mockito.Mockito.mock(ArtifactHandler.class);
        final ArtifactHandlerPublishingResult publishingResult = org.mockito.Mockito
                .mock(ArtifactHandlerPublishingResult.class);
        final ArtifactHandlerModuleDescriptor moduleDescriptor = org.mockito.Mockito.mock(ArtifactHandlerModuleDescriptor.class);
        final ImmutableChain chain = org.mockito.Mockito.mock(ImmutableChain.class);
        final ChainResultsSummary summary = org.mockito.Mockito.mock(ChainResultsSummary.class);
        final File reportDir = temporaryFolder.newFolder("report-remote");
        step("prepare a publishable report and a successful remote artifact handler", () -> {
            Files.writeString(reportDir.toPath().resolve("index.html"), "<html/>", StandardCharsets.UTF_8);
            attachDirectoryTree("Remote report before publishing", reportDir.toPath());
            doReturn(List.of(handler))
                    .when(pluginAccessor)
                    .getModules(org.mockito.ArgumentMatchers.any(java.util.function.Predicate.class));
            when(chain.getBuildDefinition()).thenReturn(buildDefinition);
            when(chain.getBuildLogger()).thenReturn(
                    org.mockito.Mockito.mock(com.atlassian.bamboo.build.logger.BuildLogger.class)
            );
            when(summary.getPlanResultKey()).thenReturn(getPlanResultKey(PLAN_KEY, Integer.parseInt(BUILD_NUMBER)));
            when(handler.canHandleArtifact(any(), any())).thenReturn(true);
            when(handler.publish(any(), any(), any(), any())).thenReturn(publishingResult);
            when(handler.getModuleDescriptor()).thenReturn(moduleDescriptor);
            when(moduleDescriptor.getCompleteKey()).thenReturn("handler-key");
            when(publishingResult.isSuccessful()).thenReturn(true);
        });

        final Optional<AllureBuildResult> result = step(
                "publish the report via the remote handler",
                () -> manager.uploadReportArtifacts(chain, summary, reportDir)
        );

        step("verify the published result carries the handler identity", () -> {
            attachText(
                    "Remote publish outcome",
                    "handlerKey=handler-key\nhandlerClass=" + handler.getClass().getName()
            );
            assertThat(result).hasValueSatisfying(buildResult -> {
                assertThat(buildResult.isSuccess()).isTrue();
                assertThat(buildResult.getArtifactHandlerClass()).isEqualTo(handler.getClass().getName());
            });
            verify(publishingResult).setArtifactHandlerKey("handler-key");
        });
    }

    @Test
    public void itShouldCleanUpOnlyOlderNumericLocalReports() throws Exception {
        final AgentLocalArtifactHandler handler = org.mockito.Mockito.mock(AgentLocalArtifactHandler.class);
        final ImmutableChain chain = org.mockito.Mockito.mock(ImmutableChain.class);
        final Path reportsDir = localStorage.resolve("allure-reports").resolve(PLAN_KEY);
        step("create a mix of numeric and non-numeric stored report directories", () -> {
            Files.createDirectories(reportsDir.resolve("1"));
            Files.createDirectories(reportsDir.resolve("2"));
            Files.createDirectories(reportsDir.resolve("10"));
            Files.createDirectories(reportsDir.resolve("abc"));
            doReturn(List.of(handler))
                    .when(pluginAccessor)
                    .getModules(org.mockito.ArgumentMatchers.any(java.util.function.Predicate.class));
            when(chain.getPlanKey()).thenReturn(getPlanKey(PLAN_KEY));
            attachDirectoryTree("Reports before cleanup", reportsDir);
        });

        step(
                "remove only reports older than the last two numeric build directories",
                () -> manager.cleanupOldReportArtifacts(chain, 2)
        );

        step("verify the latest numeric reports and named directories remain", () -> {
            attachDirectoryTree("Reports after cleanup", reportsDir);
            assertThat(reportsDir.resolve("1")).doesNotExist();
            assertThat(reportsDir.resolve("2")).exists();
            assertThat(reportsDir.resolve("10")).exists();
            assertThat(reportsDir.resolve("abc")).exists();
        });
    }

    @Test
    public void itShouldMergeAndFilterArtifactHandlerConfiguration() throws Exception {
        final Map<String, String> runtimeConfig = new HashMap<>();
        runtimeConfig.put("custom.artifactHandlers.main.url", "https://runtime.example");
        runtimeConfig.put("unrelated", "ignored");
        final Map<String, String> planConfig = new HashMap<>();
        planConfig.put("custom.artifactHandlers.useCustomArtifactHandlers", "true");
        planConfig.put("custom.artifactHandlers.main.url", "https://plan.example");
        planConfig.put("custom.artifactHandlers.other.path", "/tmp/path");
        planConfig.put("custom.artifactHandlers.main.enabledForShared", "true");
        step("provide runtime defaults and plan-specific artifact handler overrides", () -> {
            when(artifactHandlersService.getRuntimeConfiguration()).thenReturn(runtimeConfig);
            when(buildDefinition.getCustomConfiguration()).thenReturn(planConfig);
            attachText("Plan artifact handler config", planConfig.toString());
        });

        final Map<String, String> config = step(
                "merge the handler configuration and drop unsupported keys",
                () -> getArtifactHandlerConfig(buildDefinition)
        );

        step("verify plan values win and shared-only flags are filtered out", () -> {
            attachText("Merged artifact handler config", config.toString());
            assertThat(config.keySet()).containsExactlyInAnyOrder(
                    "custom.artifactHandlers.main.url",
                    "custom.artifactHandlers.other.path",
                    "custom.artifactHandlers.useCustomArtifactHandlers"
            );
            assertThat(config.get("custom.artifactHandlers.main.url")).isEqualTo("https://plan.example");
            assertThat(config.get("custom.artifactHandlers.other.path")).isEqualTo("/tmp/path");
            assertThat(config).doesNotContainKey("custom.artifactHandlers.main.enabledForShared");
        });
    }

    private ResultsSummary resultsSummaryWithHandler(final String handlerClassName) {
        final ResultsSummary resultsSummary = org.mockito.Mockito.mock(ResultsSummary.class);
        final Map<String, String> customData = new HashMap<>();
        allureBuildResult(true, null).withHandlerClass(handlerClassName).dumpToCustomData(customData);
        doReturn(customData).when(resultsSummary).getCustomBuildData();
        return resultsSummary;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getArtifactHandlerConfig(final BuildDefinition definition) throws Exception {
        final Method method = AllureArtifactsManager.class
                .getDeclaredMethod("getArtifactHandlersConfig", BuildDefinition.class);
        method.setAccessible(true);
        return (Map<String, String>) method.invoke(manager, definition);
    }
}
