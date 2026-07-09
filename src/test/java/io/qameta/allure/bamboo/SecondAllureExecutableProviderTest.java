/*
 *  Copyright 2016-2026 Qameta Software Inc
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static io.qameta.allure.bamboo.AllureExecutableProvider.BIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.junit.MockitoJUnit.rule;

public class SecondAllureExecutableProviderTest {

    private static final String ALLURE_2_21_0 = "Allure 2.21.0";
    private static final String EXECUTABLE_NAME_2_21_0 = "2.21.0";
    private final String homeDir = "/home/allure";

    @Rule
    public MockitoRule mockitoRule = rule();
    @Mock
    private BambooExecutablesManager executablesManager;
    @Mock
    private AllureDownloader downloader;
    @Mock
    private AllureCommandLineSupport cmdLine;
    @InjectMocks
    private AllureExecutableProvider provider;
    private AllureGlobalConfig config;
    private Path allureCmdPath;
    private Path allureBatCmdPath;

    @Before
    public void setUp() throws Exception {
        config = new AllureGlobalConfig();
        allureCmdPath = Paths.get(homeDir, BIN, "allure");
        allureBatCmdPath = Paths.get(homeDir, BIN, "allure.bat");
    }

    @Test
    public void itShouldProvideExecutableForUnix() throws Exception {
        when(cmdLine.hasCommand(allureCmdPath.toString())).thenReturn(true);
        when(cmdLine.isWindows()).thenReturn(false);

        final Optional<AllureExecutable> res = provide("Allure 2.0-BETA5");

        assertThat(res).isPresent();
        assertThat(res).hasValueSatisfying(executable -> assertThat(executable.getCmdPath()).isEqualTo(allureCmdPath));
    }

    @Test
    public void itShouldProvideExecutableForWindows() throws Exception {
        when(cmdLine.hasCommand(allureBatCmdPath.toString())).thenReturn(true);
        when(cmdLine.isWindows()).thenReturn(true);

        final Optional<AllureExecutable> res = provide(ALLURE_2_21_0);

        assertThat(res).isPresent();
        assertThat(res).hasValueSatisfying(executable -> assertThat(executable.getCmdPath()).isEqualTo(allureBatCmdPath));
    }

    private Optional<AllureExecutable> provide(String executableName) {
        when(executablesManager.getExecutableByName(executableName)).thenReturn(Optional.of(homeDir));
        return provider.provide(config, executableName);
    }
}
