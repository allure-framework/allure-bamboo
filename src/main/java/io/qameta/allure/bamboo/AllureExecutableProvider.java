package io.qameta.allure.bamboo;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static java.util.regex.Pattern.compile;

public class AllureExecutableProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(AllureExecutableProvider.class);
    static final String DEFAULT_VERSION = "2.2.1";
    static final String DEFAULT_PATH = "/tmp/allure/2.2.1";
    private static final Pattern EXEC_NAME_PATTERN = compile("[^\\d]*(\\d[0-9\\.]{2,}[a-zA-Z0-9\\-]*)$");
    private static final String BINARY_SUBDIR = "binary";

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

    @VisibleForTesting
    public static String getAllureSubDir() {
        return BINARY_SUBDIR;
    }

    Optional<AllureExecutable> provide(boolean isDownloadEnabled, String executableName) {
        return bambooExecutablesManager.getExecutableByName(executableName)
                .map(allureHomeDir -> {
                    LOGGER.debug("Found allure executable by name '{}': '{}'", executableName, allureHomeDir);
                    final String allureHomeSubDir = Paths.get(allureHomeDir, BINARY_SUBDIR).toString();
                    final Path cmdPath = Paths.get(allureHomeSubDir, "bin", getAllureExecutableName());
                    LOGGER.debug("Checking the existence of the command path for executable '{}': '{}'",
                            executableName, cmdPath);
                    final boolean commandExists = cmdLine.hasCommand(cmdPath.toString());
                    LOGGER.debug("System has command for executable '{}': {}, downloadEnabled={}",
                            executableName, commandExists, isDownloadEnabled);
                    if (!commandExists && isDownloadEnabled) {
                        final Matcher nameMatcher = EXEC_NAME_PATTERN.matcher(executableName);
                        allureDownloader.downloadAndExtractAllureTo(allureHomeSubDir,
                                nameMatcher.matches() ? nameMatcher.group(1) : DEFAULT_VERSION);
                    }
                    return commandExists ? new AllureExecutable(cmdPath, cmdLine) : null;
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
