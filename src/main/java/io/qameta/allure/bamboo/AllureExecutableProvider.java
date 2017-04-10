package io.qameta.allure.bamboo;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static java.util.regex.Pattern.compile;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;

public class AllureExecutableProvider {
    static final String DEFAULT_VERSION = "2.0-BETA6";
    static final String DEFAULT_PATH = "/tmp/allure-executable";
    private static final Pattern EXEC_NAME_PATTERN = compile(".+([0-9\\.]{3,}[a-zA-Z0-9\\-]*)$");

    private final BambooExecutablesManager bambooExecutablesManager;
    private final AllureDownloader allureDownloader;

    public AllureExecutableProvider(BambooExecutablesManager bambooExecutablesManager,
                                    AllureDownloader allureDownloader) {
        this.bambooExecutablesManager = requireNonNull(bambooExecutablesManager);
        this.allureDownloader = requireNonNull(allureDownloader);
    }

    Optional<AllureExecutable> provide(AllureGlobalConfig globalConfig, String executableName) {
        return bambooExecutablesManager.getExecutableByName(executableName)
                .map(allureHomeDir -> {
                    final Path cmdPath = Paths.get(allureHomeDir, "bin", getAllureExecutableName());
                    if (!cmdPath.toFile().exists() && globalConfig.isDownloadEnabled()) {
                        final Matcher nameMatcher = EXEC_NAME_PATTERN.matcher(executableName);
                        allureDownloader.downloadAndExtractAllureTo(allureHomeDir,
                                nameMatcher.matches() ? nameMatcher.group(1) : DEFAULT_VERSION);
                    }
                    return (cmdPath.toFile().exists()) ? new AllureExecutable(cmdPath) : null;
                });
    }

    @NotNull
    private String getAllureExecutableName() {
        return (IS_OS_WINDOWS) ? "allure.bat" : "allure";
    }
}
