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

import com.atlassian.bamboo.artifact.MutableArtifact;
import com.atlassian.bamboo.artifact.MutableArtifactImpl;
import com.atlassian.bamboo.build.BuildDefinition;
import com.atlassian.bamboo.build.BuildDefinitionManager;
import com.atlassian.bamboo.build.artifact.AgentLocalArtifactHandler;
import com.atlassian.bamboo.build.artifact.ArtifactFileData;
import com.atlassian.bamboo.build.artifact.ArtifactHandler;
import com.atlassian.bamboo.build.artifact.ArtifactHandlerPublishingResult;
import com.atlassian.bamboo.build.artifact.ArtifactHandlerPublishingResultImpl;
import com.atlassian.bamboo.build.artifact.ArtifactHandlingUtils;
import com.atlassian.bamboo.build.artifact.ArtifactLink;
import com.atlassian.bamboo.build.artifact.ArtifactLinkDataProvider;
import com.atlassian.bamboo.build.artifact.ArtifactLinkManager;
import com.atlassian.bamboo.build.artifact.ArtifactPublishingConfig;
import com.atlassian.bamboo.build.artifact.BambooRemoteArtifactHandler;
import com.atlassian.bamboo.build.artifact.FileSystemArtifactLinkDataProvider;
import com.atlassian.bamboo.build.artifact.TrampolineArtifactFileData;
import com.atlassian.bamboo.build.artifact.TrampolineUrlArtifactLinkDataProvider;
import com.atlassian.bamboo.build.artifact.handlers.ArtifactHandlersService;
import com.atlassian.bamboo.chains.ChainResultsSummary;
import com.atlassian.bamboo.chains.ChainStageResult;
import com.atlassian.bamboo.plan.PlanResultKey;
import com.atlassian.bamboo.plan.artifact.ArtifactDefinitionContextImpl;
import com.atlassian.bamboo.plan.cache.ImmutableChain;
import com.atlassian.bamboo.plugin.BambooPluginUtils;
import com.atlassian.bamboo.resultsummary.BuildResultsSummary;
import com.atlassian.bamboo.resultsummary.ResultsSummaryManager;
import com.atlassian.bamboo.security.SecureToken;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.predicate.EnabledModulePredicate;
import com.atlassian.plugin.predicate.ModuleOfClassPredicate;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import com.google.common.collect.ImmutableList;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.types.FileSet;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collector;

import static com.atlassian.bamboo.build.artifact.AbstractArtifactHandler.configProvider;
import static com.atlassian.bamboo.plan.PlanKeys.getPlanKey;
import static com.atlassian.bamboo.plan.PlanKeys.getPlanResultKey;
import static com.atlassian.bamboo.plugin.descriptor.ArtifactHandlerModuleDescriptor.ARTIFACT_HANDLERS_CONFIG_PREFIX;
import static com.atlassian.bamboo.plugin.descriptor.ArtifactHandlerModuleDescriptorImpl.SHARED_NON_SHARED_ONOFF_OPTION_NAME;
import static com.google.common.collect.Iterables.size;
import static io.qameta.allure.bamboo.AllureBuildResult.allureBuildResult;
import static io.qameta.allure.bamboo.AllureBuildResult.fromCustomData;
import static io.qameta.allure.bamboo.util.ExceptionUtil.stackTraceToString;
import static java.lang.Integer.parseInt;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.core.UriBuilder.fromPath;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.moveDirectory;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.codehaus.plexus.util.FileUtils.copyDirectory;
import static org.codehaus.plexus.util.FileUtils.copyURLToFile;

@SuppressWarnings({"ClassDataAbstractionCoupling",
        "PMD.AvoidInstantiatingObjectsInLoops", "PMD.NcssCount", "PMD.GodClass"})
public class AllureArtifactsManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureArtifactsManager.class);
    private static final String REPORTS_SUBDIR = "allure-reports";
    private static final String INDEX_HTML = "index.html";
    private static final String FAILED_TO_DOWNLOAD_ARTIFACTS_TO_DIR = "Failed to download artifacts to %s";
    private static final int SINGLE_NUMBER_OF_LIST_ELEMENTS = 1;

    private final PluginAccessor pluginAccessor;
    private final ArtifactHandlersService artifactHandlersService;
    private final BuildDefinitionManager buildDefinitionManager;
    private final ResultsSummaryManager resultsSummaryManager;
    private final ArtifactLinkManager artifactLinkManager;
    private final ApplicationProperties appProperties;
    private final AllureSettingsManager settingsManager;

    public AllureArtifactsManager(final PluginAccessor pluginAccessor,
                                  final ArtifactHandlersService artifactHandlersService,
                                  final BuildDefinitionManager buildDefinitionManager,
                                  final ResultsSummaryManager resultsSummaryManager,
                                  final ArtifactLinkManager artifactLinkManager,
                                  final ApplicationProperties appProperties,
                                  final AllureSettingsManager settingsManager) {
        this.pluginAccessor = pluginAccessor;
        this.artifactHandlersService = artifactHandlersService;
        this.buildDefinitionManager = buildDefinitionManager;
        this.resultsSummaryManager = resultsSummaryManager;
        this.artifactLinkManager = artifactLinkManager;
        this.appProperties = appProperties;
        this.settingsManager = settingsManager;
    }

    /**
     * Returns the signed url for the requested artifact.
     *
     * @param planKeyString key for plan
     * @param buildNumber   build number
     * @param filePath      path of the artifact
     * @return empty if cannot get artifact, url if possible
     */
    Optional<String> getArtifactUrl(final String planKeyString,
                                    final String buildNumber,
                                    final String filePath) {

        final BuildDefinition buildDefinition = buildDefinitionManager.getBuildDefinition(getPlanKey(planKeyString));
        final Map<String, String> artifactConfig = getArtifactHandlersConfig(buildDefinition);
        final PlanResultKey planResultKey = getPlanResultKey(planKeyString, parseInt(buildNumber));
        return Optional.ofNullable(resultsSummaryManager.getResultsSummary(planResultKey))
                .flatMap(resultsSummary ->
                        getArtifactHandlerByClassName(
                                fromCustomData(resultsSummary.getCustomBuildData()).getArtifactHandlerClass()
                        ).map(artifactHandler -> getArtifactUrl(
                                planKeyString, buildNumber, filePath, artifactConfig, planResultKey, artifactHandler)
                        )
                );
    }

    private String getArtifactUrl(final String planKeyString,
                                  final String buildNumber,
                                  final String filePath,
                                  final Map<String, String> artifactConfig,
                                  final PlanResultKey planResultKey,
                                  final ArtifactHandler artifactHandler) {
        if (isAgentArtifactHandler(artifactHandler)) {
            return getLocalStorageURL(planKeyString, buildNumber, filePath);
        }
        final ArtifactDefinitionContextImpl artifactDef = getAllureArtifactDef();
        final MutableArtifact artifact = mutableArtifact(planResultKey, artifactDef.getName());
        final ArtifactLinkDataProvider dataProvider = artifactHandler
                .getArtifactLinkDataProvider(artifact, configProvider(artifactConfig));
        return Optional.ofNullable(dataProvider)
                .map(linkProvider -> getArtifactUrl(filePath, planResultKey, artifactDef, linkProvider))
                .orElse(null);
    }

    private String getArtifactUrl(final String filePath,
                                  final PlanResultKey planResultKey,
                                  final ArtifactDefinitionContextImpl artifactDef,
                                  final ArtifactLinkDataProvider linkProvider) {
        if (linkProvider instanceof TrampolineUrlArtifactLinkDataProvider) {

            final TrampolineUrlArtifactLinkDataProvider urlLinkProvider =
                    (TrampolineUrlArtifactLinkDataProvider) linkProvider;

            urlLinkProvider.setPlanResultKey(planResultKey);
            urlLinkProvider.setArtifactName(artifactDef.getName());
        }
        return getArtifactFile(filePath, linkProvider);
    }

    @Nullable
    private String getLocalStorageURL(final String planKeyString,
                                      final String buildNumber,
                                      final String filePath) {
        try {
            final File file = getLocalStoragePath(planKeyString, buildNumber).resolve(filePath).toFile();
            final String fullPath = file.isDirectory()
                    ? new File(file, INDEX_HTML).getAbsolutePath()
                    : file.getAbsolutePath();
            return new File(fullPath).toURI().toURL().toString();
        } catch (Exception e) {
            // should never happen.
            throw new AllurePluginException("Unexpected error", e);
        }
    }

    /**
     * Downloads all artifacts of a build chain to a temporary directory.
     *
     * @param chainResultsSummary chain results
     * @param baseDir             temporary directory
     * @param artifactName        name of the artifact to use (all artifacts will be used if null)
     */
    @SuppressWarnings("PMD.CognitiveComplexity")
    Collection<Path> downloadAllArtifactsTo(final @NotNull ChainResultsSummary chainResultsSummary,
                                            final File baseDir,
                                            final @Nullable String artifactName) throws IOException {
        final List<Path> resultsPaths = new ArrayList<>();
        for (ChainStageResult stageResult : chainResultsSummary.getStageResults()) {
            for (BuildResultsSummary resultsSummary : stageResult.getBuildResults()) {
                LOGGER.info("Found {} artifacts totally for the build {}-{}",
                        Optional.of(resultsSummary.getProducedArtifactLinks()).map(Collection::size).orElse(0),
                        chainResultsSummary.getPlanKey(), chainResultsSummary.getBuildNumber());
                for (ArtifactLink link : resultsSummary.getProducedArtifactLinks()) {
                    final MutableArtifact artifact = link.getArtifact();
                    if (isEmpty(artifactName) || artifactName.equals(artifact.getLabel())) {
                        LOGGER.info("artifact {} matches the configured artifact name {} for the build {}-{}",
                                artifact.getLabel(), artifactName,
                                chainResultsSummary.getPlanKey(), chainResultsSummary.getBuildNumber());
                        final File stageDir = new File(baseDir, UUID.randomUUID().toString());
                        forceMkdir(stageDir);
                        resultsPaths.add(stageDir.toPath());
                        final ArtifactLinkDataProvider dataProvider =
                                artifactLinkManager.getArtifactLinkDataProvider(artifact);
                        if (dataProvider instanceof FileSystemArtifactLinkDataProvider) {
                            downloadAllArtifactsTo((FileSystemArtifactLinkDataProvider) dataProvider, stageDir);
                        } else {
                            downloadAllArtifactsTo(dataProvider, stageDir, "");
                        }
                    }
                }
            }
        }
        return resultsPaths;
    }

    private void downloadAllArtifactsTo(final ArtifactLinkDataProvider dataProvider,
                                        final File tempDir,
                                        final String startFrom) {
        for (ArtifactFileData data : requireNonNull(dataProvider).listObjects(startFrom)) {
            try {
                if (data instanceof TrampolineArtifactFileData) {
                    final TrampolineArtifactFileData trampolineData = (TrampolineArtifactFileData) data;
                    final ArtifactFileData delegateData = trampolineData.getDelegate();
                    if (delegateData.getFileType().equals(ArtifactFileData.FileType.REGULAR_FILE)) {
                        final String fileName = Paths.get(delegateData.getName()).toFile().getName();
                        if (delegateData.getUrl() != null) {
                            final URL url = new URL(delegateData.getUrl());
                            copyURLToFile(url, Paths.get(tempDir.getPath(), fileName).toFile());
                        }
                    } else {
                        downloadAllArtifactsTo(dataProvider, tempDir, trampolineData.getTag());
                    }
                }
            } catch (IOException e) {
                logAndThrow(e, String.format(FAILED_TO_DOWNLOAD_ARTIFACTS_TO_DIR, tempDir));
            }
        }
    }

    private void downloadAllArtifactsTo(final FileSystemArtifactLinkDataProvider dataProvider,
                                        final File tempDir) {
        Optional.ofNullable(dataProvider.getFile().listFiles())
                .map(Arrays::asList).ifPresent(list -> list.forEach(file -> {
                            try {
                                if (file.isFile()) {
                                    FileUtils.copyFile(file, Paths.get(tempDir.getPath(), file.getName()).toFile());
                                } else if (!StringUtils.equals(".", file.getName())
                                        && !StringUtils.equals("..", file.getName())) {
                                    copyDirectory(dataProvider.getFile(), tempDir);
                                }
                            } catch (IOException e) {
                                logAndThrow(e, String.format(FAILED_TO_DOWNLOAD_ARTIFACTS_TO_DIR, tempDir));
                            }
                        }
                ));
    }

    /**
     * Copy all the build's artifacts for this build across to the builds artifact directory.
     *
     * @param chain     chain
     * @param summary   results summary
     * @param reportDir directory of a report
     * @return empty if not applicable, result otherwise
     */
    Optional<AllureBuildResult> uploadReportArtifacts(final @NotNull ImmutableChain chain,
                                                      final @NotNull ChainResultsSummary summary,
                                                      final File reportDir) {
        try {
            final ArtifactDefinitionContextImpl artifact = getAllureArtifactDef();
            artifact.setLocation("");
            final FileSet sourceFileSet;
            sourceFileSet = ArtifactHandlingUtils.createFileSet(reportDir, artifact, true,
                    org.apache.log4j.Logger.getLogger(getClass()));
            sourceFileSet.setDir(reportDir);
            sourceFileSet.setIncludes(artifact.getCopyPattern());
            final Map<String, String> artifactConfig = getArtifactHandlersConfig(chain.getBuildDefinition());

            for (final ArtifactHandler artifactHandler : getArtifactHandlers()) {
                if (!artifactHandler.canHandleArtifact(artifact, artifactConfig)) {
                    continue;
                }
                if (isAgentArtifactHandler(artifactHandler)) {
                    final String planKey = chain.getPlanKey().getKey();
                    final String buildNumber = String.valueOf(summary.getBuildNumber());
                    final File destDir = getLocalStoragePath(planKey, buildNumber).toFile();
                    if (destDir.exists()) {
                        deleteQuietly(destDir);
                    }
                    moveDirectory(reportDir, destDir);
                    return Optional.of(allureBuildResult(true, null)
                            .withHandlerClass(artifactHandler.getClass().getName()));
                }
                final ArtifactPublishingConfig artifactPublishingConfig =
                        new ArtifactPublishingConfig(sourceFileSet, artifactConfig);
                final String errorMessage = "Unable to publish artifact via " + artifactHandler;
                final ArtifactHandlerPublishingResult publishingResult = BambooPluginUtils.callUnsafeCode(
                        new BambooPluginUtils.NoThrowCallable<ArtifactHandlerPublishingResult>(errorMessage) {

                    @NotNull
                    @Override
                    public ArtifactHandlerPublishingResult call() {
                        try {
                            return artifactHandler
                                    .publish(summary.getPlanResultKey(), artifact, artifactPublishingConfig);
                        } catch (final Exception e) {
                            LOGGER.error("Failed to publish Allure Report using handler "
                                    + artifactHandler.getClass().getName(), e);
                            return ArtifactHandlerPublishingResultImpl.failure();
                        }
                    }
                });

                if (publishingResult != null) {
                    publishingResult.setArtifactHandlerKey(artifactHandler.getModuleDescriptor().getCompleteKey());
                    return Optional.of(allureBuildResult(publishingResult.isSuccessful(), null)
                            .withHandlerClass(artifactHandler.getClass().getName()));
                }
            }
        } catch (Exception e) {
            final String message = "Failed to publish Allure Report from directory " + reportDir;
            LOGGER.error(message, e);
            return Optional.of(allureBuildResult(false, message + "\n" + stackTraceToString(e)));
        }
        return Optional.empty();
    }

    private Path getLocalStoragePath(final String planKey,
                                     final String buildNumber) {
        return Paths.get(settingsManager.getSettings().getLocalStoragePath(), REPORTS_SUBDIR, planKey, buildNumber);
    }

    private void logAndThrow(final Exception e, final String message) {
        LOGGER.error(message, e);
        throw new AllurePluginException(message, e);
    }

    @SuppressWarnings("PMD.CognitiveComplexity")
    @Nullable
    private String getArtifactFile(final String filePath,
                                   final ArtifactLinkDataProvider linkProvider) {
        if (linkProvider instanceof FileSystemArtifactLinkDataProvider) {
            return linkProvider.getRootUrl()
                    .replaceFirst("BASE_URL", getBaseUrl().build().toString())
                    .replace(INDEX_HTML, isEmpty(filePath) ? INDEX_HTML : filePath);
        } else {
            final Iterable<ArtifactFileData> datas = linkProvider.listObjects(filePath);
            if (SINGLE_NUMBER_OF_LIST_ELEMENTS == size(datas)) {
                ArtifactFileData data = datas.iterator().next();
                if (data instanceof TrampolineArtifactFileData) {
                    final TrampolineArtifactFileData trampolineData = (TrampolineArtifactFileData) data;
                    data = trampolineData.getDelegate();
                    if (data.getFileType().equals(ArtifactFileData.FileType.REGULAR_FILE)) {
                        return data.getUrl();
                    }
                } else {
                    return getBambooArtifactUrl(data);
                }
            } else if (SINGLE_NUMBER_OF_LIST_ELEMENTS < size(datas)) {
                return getArtifactFile(INDEX_HTML, linkProvider);
            }
        }
        return null;
    }

    private String getBambooArtifactUrl(final ArtifactFileData data) {
        return Optional.ofNullable(data.getUrl())
                .map(url -> url.startsWith("/") ? getBaseUrl().path(url).build().toString() : url)
                .orElse(null);
    }

    private UriBuilder getBaseUrl() {
        return fromPath(appProperties.getBaseUrl(UrlMode.ABSOLUTE));
    }

    @NotNull
    private ArtifactDefinitionContextImpl getAllureArtifactDef() {
        final ArtifactDefinitionContextImpl artifact = new ArtifactDefinitionContextImpl(
                "allure-report", false, SecureToken.create());
        artifact.setCopyPattern("**/**");
        return artifact;
    }

    @NotNull
    private Map<String, String> getArtifactHandlersConfig(final BuildDefinition buildDefinition) {
        final Map<String, String> config = artifactHandlersService.getRuntimeConfiguration();
        final Map<String, String> planCustomConfiguration = buildDefinition.getCustomConfiguration();
        if (ArtifactHandlingUtils.isCustomArtifactHandlingConfigured(planCustomConfiguration)) {
            // This hacky way it's compatible with both Bamboo 5.x and Bamboo 6.x
            final Collector<Map.Entry<String, String>, ?, Map<String, String>> toMap =
                    toMap(Map.Entry::getKey, Map.Entry::getValue);
            final Predicate<Map.Entry<String, String>> isArtifactHandler =
                    e -> e.getKey().startsWith(ARTIFACT_HANDLERS_CONFIG_PREFIX);
            final Predicate<Map.Entry<String, String>> isNotHandlerSwitch =
                    e -> SHARED_NON_SHARED_ONOFF_OPTION_NAME.values().stream().noneMatch(o -> e.getKey().endsWith(o));
            config.putAll(planCustomConfiguration.entrySet().stream().filter(isArtifactHandler).collect(toMap));
            return config.entrySet().stream().filter(isArtifactHandler).filter(isNotHandlerSwitch).collect(toMap);
        }
        return config;
    }

    @NotNull
    private MutableArtifact mutableArtifact(final PlanResultKey planResultKey,
                                            final String name) {
        return new MutableArtifactImpl(name, planResultKey, null, false, 0L);
    }

    private List<ArtifactHandler> getArtifactHandlers() {

        Predicate<ModuleDescriptor<ArtifactHandler>> predicate = new ModuleOfClassPredicate<>(ArtifactHandler.class);
        predicate = predicate.and(new EnabledModulePredicate());

        return ImmutableList.copyOf(pluginAccessor.getModules(predicate));
    }


    private boolean isAgentArtifactHandler(final ArtifactHandler artifactHandler) {
        return artifactHandler instanceof BambooRemoteArtifactHandler
                || artifactHandler instanceof AgentLocalArtifactHandler;
    }

    @SuppressWarnings("unchecked")
    private <T extends ArtifactHandler> Optional<T> getArtifactHandlerByClassName(final String className) {
        return Optional.ofNullable(className).map(clazz -> {
            final Class<T> aClass;
            try {
                aClass = (Class<T>) Class.forName(clazz);
                Predicate<ModuleDescriptor<T>> predicate = new ModuleOfClassPredicate<T>(aClass);
                predicate = predicate.and(new EnabledModulePredicate());

                return pluginAccessor.getModules(predicate).stream().findAny().orElse(null);
            } catch (ClassNotFoundException e) {
                LOGGER.error("Failed to find artifact handler for class name " + className, e);
            }
            return null;
        });
    }
}
