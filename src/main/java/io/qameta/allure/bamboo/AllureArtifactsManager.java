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
import com.atlassian.bamboo.build.artifact.handlers.ArtifactHandlersFunctions;
import com.atlassian.bamboo.build.artifact.handlers.ArtifactHandlersService;
import com.atlassian.bamboo.chains.Chain;
import com.atlassian.bamboo.chains.ChainResultsSummary;
import com.atlassian.bamboo.chains.ChainStageResult;
import com.atlassian.bamboo.plan.PlanResultKey;
import com.atlassian.bamboo.plan.artifact.ArtifactDefinitionContextImpl;
import com.atlassian.bamboo.plugin.BambooPluginUtils;
import com.atlassian.bamboo.plugin.descriptor.predicate.ConjunctionModuleDescriptorPredicate;
import com.atlassian.bamboo.resultsummary.BuildResultsSummary;
import com.atlassian.bamboo.resultsummary.ResultsSummaryManager;
import com.atlassian.bamboo.security.SecureToken;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.predicate.EnabledModulePredicate;
import com.atlassian.plugin.predicate.ModuleOfClassPredicate;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.apache.tools.ant.types.FileSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.atlassian.bamboo.build.artifact.AbstractArtifactHandler.configProvider;
import static com.atlassian.bamboo.plan.PlanKeys.getPlanKey;
import static com.atlassian.bamboo.plan.PlanKeys.getPlanResultKey;
import static com.google.common.collect.Iterables.size;
import static com.google.common.io.Files.copy;
import static io.qameta.allure.bamboo.AllureBuildResult.allureBuildResult;
import static io.qameta.allure.bamboo.AllureBuildResult.fromCustomData;
import static io.qameta.allure.bamboo.util.ExceptionUtil.stackTraceToString;
import static java.lang.Integer.parseInt;
import static java.util.Optional.ofNullable;
import static javax.ws.rs.core.UriBuilder.fromPath;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.moveDirectory;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.codehaus.plexus.util.FileUtils.copyDirectory;
import static org.codehaus.plexus.util.FileUtils.copyURLToFile;

public class AllureArtifactsManager {
    private static final Logger LOGGER = Logger.getLogger(AllureArtifactsManager.class);
    private static final String REPORTS_SUBDIR = "allure-reports";

    private final PluginAccessor pluginAccessor;
    private final ArtifactHandlersService artifactHandlersService;
    private final BuildDefinitionManager buildDefinitionManager;
    private final ResultsSummaryManager resultsSummaryManager;
    private final ArtifactLinkManager artifactLinkManager;
    private final ApplicationProperties appProperties;
    private final AllureSettingsManager settingsManager;

    public AllureArtifactsManager(PluginAccessor pluginAccessor, ArtifactHandlersService artifactHandlersService,
                                  BuildDefinitionManager buildDefinitionManager, ResultsSummaryManager resultsSummaryManager,
                                  ArtifactLinkManager artifactLinkManager, ApplicationProperties appProperties,
                                  AllureSettingsManager settingsManager) {
        this.pluginAccessor = pluginAccessor;
        this.artifactHandlersService = artifactHandlersService;
        this.buildDefinitionManager = buildDefinitionManager;
        this.resultsSummaryManager = resultsSummaryManager;
        this.artifactLinkManager = artifactLinkManager;
        this.appProperties = appProperties;
        this.settingsManager = settingsManager;
    }

    /**
     * Returns the signed url for the requested artifact
     *
     * @param planKeyString key for plan
     * @param buildNumber   build number
     * @param filePath      path of the artifact
     * @return empty if cannot get artifact, url if possible
     */
    Optional<String> getArtifactUrl(final String planKeyString, final String buildNumber, final String filePath) {
        final BuildDefinition buildDefinition = buildDefinitionManager.getBuildDefinition(getPlanKey(planKeyString));
        final Map<String, String> artifactConfig = getArtifactHandlersConfig(buildDefinition);
        final PlanResultKey planResultKey = getPlanResultKey(planKeyString, parseInt(buildNumber));
        return ofNullable(resultsSummaryManager.getResultsSummary(planResultKey)).map(resultsSummary ->
                getArtifactHandlerByClassName(fromCustomData(resultsSummary.getCustomBuildData()).getArtifactHandlerClass())
                        .map(artifactHandler -> {
                            if (isAgentArtifactHandler(artifactHandler)) {
                                return getLocalStorageURL(planKeyString, buildNumber, filePath);
                            }
                            final ArtifactDefinitionContextImpl artifactDef = getAllureArtifactDef();
                            return ofNullable(artifactHandler.getArtifactLinkDataProvider(
                                    mutableArtifact(planResultKey, artifactDef.getName()), configProvider(artifactConfig)
                            )).map(linkProvider -> {
                                if (linkProvider instanceof TrampolineUrlArtifactLinkDataProvider) {
                                    final TrampolineUrlArtifactLinkDataProvider urlLinkProvider = (TrampolineUrlArtifactLinkDataProvider) linkProvider;
                                    urlLinkProvider.setPlanResultKey(planResultKey);
                                    urlLinkProvider.setArtifactName(artifactDef.getName());
                                }
                                return getArtifactFile(filePath, linkProvider);
                            }).orElse(null);
                        }).orElse(null));
    }

    @Nullable
    private String getLocalStorageURL(String planKeyString, String buildNumber, String filePath) {
        final File file = getLocalStoragePath(planKeyString, buildNumber).resolve(filePath).toFile();
        final String fullPath = (file.isDirectory()) ? new File(file, "index.html").getAbsolutePath() : file.getAbsolutePath();
        return String.format("file://%s", fullPath);
    }

    /**
     * Downloads all artifacts of a build chain to a temporary directory
     *
     * @param chainResultsSummary chain results
     * @param tempFolder          temporary directory
     */
    void downloadAllArtifactsTo(@NotNull ChainResultsSummary chainResultsSummary, File tempFolder) {
        for (ChainStageResult stageResult : chainResultsSummary.getStageResults()) {
            for (BuildResultsSummary resultsSummary : stageResult.getBuildResults()) {
                for (ArtifactLink link : resultsSummary.getArtifactLinks()) {
                    final ArtifactLinkDataProvider dataProvider = artifactLinkManager.getArtifactLinkDataProvider(link.getArtifact());
                    if (dataProvider instanceof FileSystemArtifactLinkDataProvider) {
                        downloadAllArtifactsTo((FileSystemArtifactLinkDataProvider) dataProvider, tempFolder);
                    } else {
                        downloadAllArtifactsTo(dataProvider, tempFolder, "");
                    }
                }
            }
        }
    }

    /**
     * Copy all of the build's artifacts for this build across to the builds artifact directory
     *
     * @param chain     chain
     * @param summary   results summary
     * @param reportDir directory of a report
     * @return empty if not applicable, result otherwise
     */
    Optional<AllureBuildResult> uploadReportArtifacts(@NotNull Chain chain, @NotNull ChainResultsSummary summary, File reportDir) {
        try {
            final ArtifactDefinitionContextImpl artifact = getAllureArtifactDef();
            artifact.setLocation("");
            final FileSet sourceFileSet;
            sourceFileSet = ArtifactHandlingUtils.createFileSet(reportDir, artifact, true, LOGGER);
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
                final ArtifactPublishingConfig artifactPublishingConfig = new ArtifactPublishingConfig(sourceFileSet, artifactConfig);
                final String errorMessage = "Unable to publish artifact via " + artifactHandler;
                final ArtifactHandlerPublishingResult publishingResult = BambooPluginUtils.callUnsafeCode(new BambooPluginUtils.NoThrowCallable<ArtifactHandlerPublishingResult>(errorMessage) {
                    @NotNull
                    @Override
                    public ArtifactHandlerPublishingResult call() {
                        try {
                            return artifactHandler.publish(summary.getPlanResultKey(), artifact, artifactPublishingConfig);
                        } catch (final Exception e) {
                            LOGGER.error("Failed to publish Allure Report using handler " + artifactHandler.getClass().getName(), e);
                            return ArtifactHandlerPublishingResultImpl.failure();
                        }
                    }
                });
                if (publishingResult == null) {
                    continue;
                }
                publishingResult.setArtifactHandlerKey(artifactHandler.getModuleDescriptor().getCompleteKey());
                return Optional.of(allureBuildResult(publishingResult.isSuccessful(), null)
                        .withHandlerClass(artifactHandler.getClass().getName()));
            }
        } catch (Exception e) {
            final String message = "Failed to publish Allure Report from directory " + reportDir;
            LOGGER.error(message, e);
            return Optional.of(allureBuildResult(false, message + "\n" + stackTraceToString(e)));
        }
        return Optional.empty();
    }

    private Path getLocalStoragePath(String planKey, String buildNumber) {
        return Paths.get(settingsManager.getSettings().getLocalStoragePath(), REPORTS_SUBDIR, planKey, buildNumber);
    }

    private void downloadAllArtifactsTo(ArtifactLinkDataProvider dataProvider, File tempDir, String startFrom) {
        for (ArtifactFileData data : dataProvider.listObjects(startFrom)) {
            try {
                if (data instanceof TrampolineArtifactFileData) {
                    final TrampolineArtifactFileData trampolineData = (TrampolineArtifactFileData) data;
                    data = trampolineData.getDelegate();
                    if (data.getFileType().equals(ArtifactFileData.FileType.REGULAR_FILE)) {
                        final String fileName = Paths.get(data.getName()).toFile().getName();
                        copyURLToFile(new URL(data.getUrl()), Paths.get(tempDir.getPath(), fileName).toFile());
                    } else {
                        downloadAllArtifactsTo(dataProvider, tempDir, trampolineData.getTag());
                    }
                }
            } catch (IOException e) {
                logAndThrow(e, "Failed to download artifacts to " + tempDir);
            }
        }
    }

    private void logAndThrow(Exception e, String message) {
        LOGGER.error(message, e);
        throw new RuntimeException(message, e);
    }

    private void downloadAllArtifactsTo(FileSystemArtifactLinkDataProvider dataProvider, File tempDir) {
        ofNullable(dataProvider.getFile().listFiles()).map(Arrays::asList).ifPresent(list -> list.forEach(file -> {
                    try {
                        if (file.isFile()) {
                            copy(file, Paths.get(tempDir.getPath(), file.getName()).toFile());
                        } else if (!file.getName().equals(".") && !file.getName().equals("..")) {
                            copyDirectory(dataProvider.getFile(), tempDir);
                        }
                    } catch (IOException e) {
                        logAndThrow(e, "Failed to download artifacts to " + tempDir);
                    }
                }
        ));
    }

    @Nullable
    private String getArtifactFile(String filePath, ArtifactLinkDataProvider linkProvider) {
        if (linkProvider instanceof FileSystemArtifactLinkDataProvider) {
            return linkProvider.getRootUrl()
                    .replaceFirst("BASE_URL", getBaseUrl().build().toString())
                    .replace("index.html", isEmpty(filePath) ? "index.html" : filePath);
        } else {
            final Iterable<ArtifactFileData> datas = linkProvider.listObjects(filePath);
            if (size(datas) == 1) {
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
            } else if (size(datas) > 1) {
                return getArtifactFile("index.html", linkProvider);
            }
        }
        return null;
    }

    private String getBambooArtifactUrl(ArtifactFileData data) {
        return ofNullable(data.getUrl()).map(url -> (url.startsWith("/")) ?
                getBaseUrl().path(url).build().toString() : url)
                .map(url -> url)
                .orElse(null);
    }

    private UriBuilder getBaseUrl() {
        return fromPath(appProperties.getBaseUrl(UrlMode.ABSOLUTE));
    }

    @NotNull
    private ArtifactDefinitionContextImpl getAllureArtifactDef() {
        final ArtifactDefinitionContextImpl artifact = new ArtifactDefinitionContextImpl("allure-report", false, SecureToken.create());
        artifact.setCopyPattern("**/**");
        return artifact;
    }

    @NotNull
    private Map<String, String> getArtifactHandlersConfig(BuildDefinition buildDefinition) {
        final Map<String, String> config = artifactHandlersService.getRuntimeConfiguration();
        final Map<String, String> planCustomConfiguration = buildDefinition.getCustomConfiguration();
        if (ArtifactHandlingUtils.isCustomArtifactHandlingConfigured(planCustomConfiguration)) {
            Iterables.removeIf(config.keySet(), ArtifactHandlersFunctions.isArtifactHandlerOnOffSwitch());
            final Map<String, String> planLevelConfiguration = Maps.filterKeys(planCustomConfiguration, ArtifactHandlersFunctions.isArtifactHandlerConfiguration());
            config.putAll(planLevelConfiguration);
        }
        return config;
    }

    @NotNull
    private MutableArtifact mutableArtifact(PlanResultKey planResultKey, String name) {
        return new MutableArtifactImpl(name, planResultKey, null, false, 0L);
    }

    private List<ArtifactHandler> getArtifactHandlers() {
        final ConjunctionModuleDescriptorPredicate<ArtifactHandler> predicate = new ConjunctionModuleDescriptorPredicate<>();

        predicate.append(new ModuleOfClassPredicate<>(ArtifactHandler.class));
        predicate.append(new EnabledModulePredicate<>());

        return ImmutableList.copyOf(pluginAccessor.getModules(predicate));
    }


    private boolean isAgentArtifactHandler(ArtifactHandler artifactHandler) {
        return artifactHandler instanceof BambooRemoteArtifactHandler || artifactHandler instanceof AgentLocalArtifactHandler;
    }

    @SuppressWarnings("unchecked")
    private <T extends ArtifactHandler> Optional<T> getArtifactHandlerByClassName(String className) {
        final ConjunctionModuleDescriptorPredicate<T> predicate = new ConjunctionModuleDescriptorPredicate<>();
        return ofNullable(className).map(clazz -> {
            final Class<T> aClass;
            try {
                aClass = (Class<T>) Class.forName(clazz);
                predicate.append(new ModuleOfClassPredicate<>(aClass));
                predicate.append(new EnabledModulePredicate<>());

                return pluginAccessor.getModules(predicate).stream().findAny().orElse(null);
            } catch (ClassNotFoundException e) {
                LOGGER.error("Failed to find artifact handler for class name " + className, e);
            }
            return null;
        });
    }
}
