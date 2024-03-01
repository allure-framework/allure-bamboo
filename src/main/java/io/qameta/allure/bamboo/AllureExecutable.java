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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.qameta.allure.bamboo.info.AllurePlugins;
import io.qameta.allure.bamboo.util.FileStringReplacer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.Pattern;

import static java.nio.file.Files.createTempDirectory;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FileUtils.copyDirectoryToDirectory;

class AllureExecutable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureExecutable.class);
    private static final String BASH_CMD = "/bin/bash";
    private final Path cmdPath;
    private final AllureCommandLineSupport cmdLine;

    AllureExecutable(final Path cmdPath,
                     final AllureCommandLineSupport commandLine) {
        this.cmdPath = cmdPath;
        this.cmdLine = commandLine;
    }

    @Nonnull
    AllureGenerateResult generate(final Collection<Path> sourceDirs,
                                  final Path targetDir) {
        try {
            final LinkedList<String> args = new LinkedList<>(asList("generate", "-o", targetDir.toString()));
            args.addAll(sourceDirs.stream().map(Path::toString).collect(toList()));
            final String output;
            if (cmdLine.isUnix() && cmdLine.hasCommand(BASH_CMD)) {
                args.addFirst(cmdPath.toString());
                output = cmdLine.runCommand(BASH_CMD, args.toArray(new String[0]));
            } else {
                output = cmdLine.runCommand(cmdPath.toString(), args.toArray(new String[0]));
            }
            LOGGER.info(output);
            return cmdLine.parseGenerateOutput(output);
        } catch (Exception e) {
            throw new AllurePluginException("Failed to generate allure report", e);
        }
    }

    public void setCustomLogo(final String logoUrl) {
        final String pluginName = "custom-logo-plugin";
        final String allureConfigFileName = "allure.yml";
        final String cssFileName = "styles.css";

        final Path rootPath = this.cmdPath.getParent().getParent();
        final Path configFolder = rootPath.resolve("config");
        final Path logoPluginFolder = rootPath.resolve("plugins").resolve(pluginName).resolve("static");

        /// Editing Yaml to add plugin
        final ObjectMapper objectMapper = new YAMLMapper();
        try {
            final File configFile = configFolder.resolve(allureConfigFileName).toFile();
            final AllurePlugins ap = objectMapper.readValue(configFile, AllurePlugins.class);
            //Saving the file only if it necessary
            if (ap.registerPlugin(pluginName)) {
                objectMapper.writeValue(configFile, ap);
            }
            //Setting new Logo
            FileStringReplacer.replaceInFile(logoPluginFolder.resolve(cssFileName),
                    Pattern.compile("url\\('.+'\\)",
                            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.COMMENTS),
                    "url(" + logoUrl + ")"
            );

            // aligning logo to center
            FileStringReplacer.replaceInFile(logoPluginFolder.resolve(cssFileName),
                    Pattern.compile("(?<=\\s )left",
                            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.COMMENTS),
                    "center"
            );
            // fit logo to area
            FileStringReplacer.replaceInFile(logoPluginFolder.resolve(cssFileName),
                    Pattern.compile("(?<=\\s )!important;",
                            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.COMMENTS),
                    "!important; background-size: contain !important;"
            );
            // removing margin
            FileStringReplacer.replaceInFile(logoPluginFolder.resolve(cssFileName),
                    Pattern.compile("10px",
                            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.COMMENTS),
                    "0px"
            );

        } catch (IOException e) {
            LOGGER.error(e.toString());
            throw new AllurePluginException("Unexpected error", e);
        }
    }

    public AllureExecutable getCopy() throws IOException {
        final String binary = this.cmdPath.getFileName().toString();
        final String binFolder = this.cmdPath.getParent().getFileName().toString();
        final Path rootPath = this.cmdPath.getParent().getParent();
        final Path rootFolderName = rootPath.getFileName();
        final Path copyPath = createTempDirectory(rootFolderName.toString());
        copyDirectoryToDirectory(rootPath.toFile(), copyPath.toFile());

        return new AllureExecutable(copyPath
                .resolve(rootFolderName.toString())
                .resolve(binFolder)
                .resolve(binary),
                this.cmdLine
        );
    }

    Path getCmdPath() {
        return cmdPath;
    }
}
