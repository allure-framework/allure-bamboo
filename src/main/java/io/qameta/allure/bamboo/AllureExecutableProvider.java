package io.qameta.allure.bamboo;

import io.qameta.allure.bamboo.config.AllureGlobalConfig;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

public class AllureExecutableProvider {
    static final String DEFAULT_VERSION = "2.0-BETA5";
    static final String DEFAULT_PATH = "/tmp/allure-executable";
    private static final Pattern EXEC_NAME_PATTERN = compile(".+([0-9\\.]{3,}[a-zA-Z0-9\\-]*)$");

    private final BambooExecutablesManager bambooExecutablesManager;
    private final AllureDownloader allureDownloader;

    public AllureExecutableProvider(BambooExecutablesManager bambooExecutablesManager, AllureDownloader allureDownloader) {
        this.bambooExecutablesManager = bambooExecutablesManager;
        this.allureDownloader = allureDownloader;
    }

    Optional<AllureExecutable> provide(AllureGlobalConfig globalConfig, String executableName) {
        return bambooExecutablesManager.getExecutableByName(executableName)
                .map(allureHomeDir -> {
                    final Path cmdPath = Paths.get(allureHomeDir, "bin", "allure");
                    if (!cmdPath.toFile().exists() && globalConfig.isDownloadEnabled()) {
                        final Matcher nameMatcher = EXEC_NAME_PATTERN.matcher(executableName);
                        allureDownloader.downloadAndExtractAllureTo(allureHomeDir,
                                nameMatcher.matches() ? nameMatcher.group(1) : DEFAULT_VERSION);
                    }
                    return (cmdPath.toFile().exists()) ? new AllureExecutable(cmdPath) : null;
                });
    }
}
