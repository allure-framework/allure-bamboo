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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.google.common.io.Files.createTempDir;
import static java.util.Collections.singleton;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.junit.MockitoJUnit.rule;

public class AllureExecutableTest {
    private static final String OPTIONS = "-o";
    private static final String GENERATE = "generate";
    private static final String BIN_BASH = "/bin/bash";
    @Rule
    public MockitoRule mockitoRule = rule();

    private final Path path = Paths.get("/tmp/where-allure/installed");

    @Mock
    private AllureCommandLineSupport cmdLine;

    private AllureExecutable executable;
    private Path fromDir;
    private Path toDir;

    @Before
    @SuppressWarnings("UnstableApiUsage")
    public void setUp() throws Exception {
        executable = new AllureExecutable(path, cmdLine);
        fromDir = createTempDir().toPath();
        toDir = createTempDir().toPath();
    }

    @Test
    public void itShouldInvokeAllureGenerateOnUnixWithBash() throws Exception {
        when(cmdLine.hasCommand(BIN_BASH))
                .thenReturn(true);
        when(cmdLine.isUnix())
                .thenReturn(true);

        executable.generate(singleton(fromDir), toDir);

        verify(cmdLine)
                .runCommand(BIN_BASH, path.toString(), GENERATE, OPTIONS, toDir.toString(), fromDir.toString());
    }

    @Test
    public void itShouldInvokeAllureGenerateOnUnixWithoutBash() throws Exception {
        when(cmdLine.hasCommand(BIN_BASH))
                .thenReturn(false);
        when(cmdLine.isUnix())
                .thenReturn(true);

        executable.generate(singleton(fromDir), toDir);

        verify(cmdLine)
                .runCommand(path.toString(), GENERATE, OPTIONS, toDir.toString(), fromDir.toString());
    }

    @Test
    public void itShouldInvokeAllureGenerateOnWindows() throws Exception {
        when(cmdLine.isUnix())
                .thenReturn(false);

        executable.generate(singleton(fromDir), toDir);

        verify(cmdLine)
                .runCommand(path.toString(), GENERATE, OPTIONS, toDir.toString(), fromDir.toString());

    }
}
