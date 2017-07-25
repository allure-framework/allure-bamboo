package io.qameta.allure.bamboo;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static java.util.regex.Pattern.compile;

public class AllureExecutableProvider {
    static final String DEFAULT_VERSION = "2.2.1";
    static final String DEFAULT_PATH = "/tmp/allure/2.2.1";
    private static final Pattern EXEC_NAME_PATTERN = compile("[^\\d]*(\\d[0-9\\.]{2,}[a-zA-Z0-9\\-]*)$");

    private final BambooExecutablesManager bambooExecutablesManager;
    private final AllureDownloader allureDownloader;
    private final AllureCommandLineSupport cmdLine;

    public AllureExecutableProvider(BambooExecutablesManager bambooExecutablesManager,
                                    AllureDownloader allureDownloader,
                                    AllureCommandLineSupport cmdLine) {
        this.bambooExecutablesManager = requireNonNull(bambooExecutablesManager);
        this.allureDownloader = requireNonNull(allureDownloader);
        this.cmdLine = requireNonNull(cmdLine);
    }

    Optional<AllureExecutable> provide(boolean isDownloadEnabled, String executableName) {
        return bambooExecutablesManager.getExecutableByName(executableName)
                .map(allureHomeDir -> {
                    final Path cmdPath = Paths.get(allureHomeDir, "bin", getAllureExecutableName());
                    if (!cmdLine.hasCommand(cmdPath.toString()) && isDownloadEnabled) {
                        final Matcher nameMatcher = EXEC_NAME_PATTERN.matcher(executableName);
                        allureDownloader.downloadAndExtractAllureTo(allureHomeDir,
                                nameMatcher.matches() ? nameMatcher.group(1) : DEFAULT_VERSION);
                    }
                    return (cmdLine.hasCommand(cmdPath.toString())) ? new AllureExecutable(cmdPath, cmdLine) : null;
                });
    }

    Optional<AllureExecutable> provide(AllureGlobalConfig globalConfig, String executableName) {
        return provide(globalConfig.isDownloadEnabled(), executableName);
    }

    @NotNull
    private String getAllureExecutableName() {
        return (cmdLine.isWindows()) ? "allure.bat" : "allure";
    }
}
