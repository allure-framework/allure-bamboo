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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

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

    Path getCmdPath() {
        return cmdPath;
    }
}
