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
    static final String DEFAULT_VERSION = "2.21.0";
    static final String DEFAULT_PATH = "/tmp/allure/2.21.0";
    private static final Pattern EXEC_NAME_PATTERN = compile("[^\\d]*(\\d[0-9\\.]{2,}[a-zA-Z0-9\\-]*)$");
    private static final String BINARY_SUBDIR = "binary";

    private final BambooExecutablesManager bambooExecutablesManager;
    private final AllureDownloader allureDownloader;
    private final AllureCommandLineSupport cmdLine;

    public AllureExecutableProvider(final BambooExecutablesManager bambooExecutablesManager,
                                    final AllureDownloader allureDownloader,
                                    final AllureCommandLineSupport cmdLine) {
        this.bambooExecutablesManager = requireNonNull(bambooExecutablesManager);
        this.allureDownloader = requireNonNull(allureDownloader);
        this.cmdLine = requireNonNull(cmdLine);
    }

    @VisibleForTesting
    public static String getAllureSubDir() {
        return BINARY_SUBDIR;
    }

    Optional<AllureExecutable> provide(final boolean isDownloadEnabled, final String executableName) {
        return bambooExecutablesManager.getExecutableByName(executableName)
                .map(allureHomeDir -> {
                    LOGGER.debug("Found allure executable by name '{}': '{}'", executableName, allureHomeDir);
                    final String allureHomeSubDir = Paths.get(allureHomeDir, BINARY_SUBDIR).toString();
                    final Path cmdPath = Paths.get(allureHomeSubDir, "bin", getAllureExecutableName());
                    final AllureExecutable executable = new AllureExecutable(cmdPath, cmdLine);
                    LOGGER.debug("Checking the existence of the command path for executable '{}': '{}'",
                            executableName, cmdPath);
                    final boolean commandExists = cmdLine.hasCommand(cmdPath.toString());
                    LOGGER.debug("System has command for executable '{}': {}, downloadEnabled={}",
                            executableName, commandExists, isDownloadEnabled);
                    if (commandExists) {
                        return executable;
                    } else if (isDownloadEnabled) {
                        final Matcher nameMatcher = EXEC_NAME_PATTERN.matcher(executableName);
                        return allureDownloader.downloadAndExtractAllureTo(allureHomeSubDir,
                                        nameMatcher.matches() ? nameMatcher.group(1) : DEFAULT_VERSION)
                                .map(path -> executable).orElse(null);
                    }
                    return null;
                });
    }

    Optional<AllureExecutable> provide(final AllureGlobalConfig globalConfig,
                                       final String executableName) {
        return provide(globalConfig.isDownloadEnabled(), executableName);
    }

    @NotNull
    private String getAllureExecutableName() {
        return (cmdLine.isWindows()) ? "allure.bat" : "allure";
    }
}
