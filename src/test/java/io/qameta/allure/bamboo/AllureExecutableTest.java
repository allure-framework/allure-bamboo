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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.qameta.allure.bamboo.TestSupport.attachText;
import static io.qameta.allure.bamboo.TestSupport.step;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
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
    private AllureExecutable customLogoExecutable;
    private Path fromDir;
    private Path toDir;

    @Before
    @SuppressWarnings("UnstableApiUsage")
    public void setUp() throws Exception {
        executable = new AllureExecutable(path, cmdLine);
        fromDir = Files.createTempDirectory("tmp_from");
        toDir = Files.createTempDirectory("tmp_to");
    }

    @Test
    public void itShouldInvokeAllureGenerateOnUnixWithBash() throws Exception {
        step("prepare a Unix environment with bash available", () -> {
            when(cmdLine.hasCommand(BIN_BASH))
                    .thenReturn(true);
            when(cmdLine.isUnix())
                    .thenReturn(true);
            attachText("Executable invocation context",
                    "binary=" + path + "\nsourceDir=" + fromDir + "\ntargetDir=" + toDir);
        });

        step("generate the report through the bash launcher",
                () -> executable.generate(singleton(fromDir), toDir));

        step("verify the command line support receives the bash invocation", () ->
                verify(cmdLine)
                        .runCommand(BIN_BASH, path.toString(), GENERATE, OPTIONS, toDir.toString(),
                                fromDir.toString()));
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

    @Test
    public void itShouldNotWriteUnsafeCustomLogoUrlIntoCss() throws Exception {
        final Path css = prepareCustomLogoLayout();

        customLogoExecutable.setCustomLogo("https://evil.example/logo.svg'); } body { display: none } /*");

        final String content = Files.readString(css);
        attachText("Custom logo stylesheet after an unsafe URL", content);
        assertThat(content).contains("url('default')");
        assertThat(content).doesNotContain("display: none");
    }

    @Test
    public void itShouldWriteSafeCustomLogoUrlIntoCss() throws Exception {
        final Path css = prepareCustomLogoLayout();

        customLogoExecutable.setCustomLogo("https://cdn.example.com/logo.svg");

        final String content = Files.readString(css);
        attachText("Custom logo stylesheet after a safe URL", content);
        assertThat(content).contains("url(https://cdn.example.com/logo.svg)");
    }

    private Path prepareCustomLogoLayout() throws IOException {
        final Path root = Files.createTempDirectory("allure-dist");
        final Path cmdPath = Files.createDirectories(root.resolve("bin")).resolve("allure");
        Files.createFile(cmdPath);
        Files.writeString(Files.createDirectories(root.resolve("config")).resolve("allure.yml"), "plugins: []\n");
        final Path staticDir = Files.createDirectories(
                root.resolve("plugins").resolve("custom-logo-plugin").resolve("static"));
        final Path css = staticDir.resolve("styles.css");
        Files.writeString(css, ".side-nav__brand { background: url('default'); left: 10px !important; }\n");
        customLogoExecutable = new AllureExecutable(cmdPath, cmdLine);
        return css;
    }
}
