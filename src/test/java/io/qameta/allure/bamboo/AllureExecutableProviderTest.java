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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static io.qameta.allure.bamboo.AllureExecutableProvider.DEFAULT_VERSION;
import static io.qameta.allure.bamboo.AllureExecutableProvider.getAllureSubDir;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.junit.MockitoJUnit.rule;

public class AllureExecutableProviderTest {

    private static final String ALLURE_2_0_0 = "Allure 2.0.0";
    private static final String EXECUTABLE_NAME_2_0_0 = "2.0.0";
    private static final String BIN = "bin";
    private final String homeDir = "/home/allure";
    private final String binaryDir = Paths.get(this.homeDir, getAllureSubDir()).toString();

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
        allureCmdPath = Paths.get(binaryDir, BIN, "allure");
        allureBatCmdPath = Paths.get(binaryDir, BIN, "allure.bat");
        when(downloader.downloadAndExtractAllureTo(anyString(), anyString())).thenReturn(Optional.empty());
    }

    @Test
    public void itShouldProvideDefaultVersion() throws Exception {
        provide("Allure WITHOUT VERSION");
        verify(downloader).downloadAndExtractAllureTo(binaryDir, DEFAULT_VERSION);
    }

    @Test
    public void itShouldProvideTheGivenVersionWithFullSemverWithoutName() throws Exception {
        provide(EXECUTABLE_NAME_2_0_0);
        verify(downloader).downloadAndExtractAllureTo(binaryDir, EXECUTABLE_NAME_2_0_0);
    }

    @Test
    public void itShouldProvideTheGivenVersionWithFullSemverWithoutMilestone() throws Exception {
        provide(ALLURE_2_0_0);
        verify(downloader).downloadAndExtractAllureTo(binaryDir, EXECUTABLE_NAME_2_0_0);
    }

    @Test
    public void itShouldProvideTheGivenVersionWithMajorMinorWithoutMilestone() throws Exception {
        provide("Allure 2.0");
        verify(downloader).downloadAndExtractAllureTo(binaryDir, "2.0");
    }

    @Test
    public void itShouldProvideTheGivenVersionWithMilestone() throws Exception {
        provide("Allure 2.0-BETA4");
        verify(downloader).downloadAndExtractAllureTo(binaryDir, "2.0-BETA4");
    }

    private Optional<AllureExecutable> provide(String executableName) {
        when(executablesManager.getExecutableByName(executableName)).thenReturn(Optional.of(homeDir));
        return provider.provide(config, executableName);
    }
}
