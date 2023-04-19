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

import io.qameta.allure.bamboo.util.FilesUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;

import java.nio.file.Paths;

import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.junit.MockitoJUnit.rule;

public class AllureDownloaderTest {

    @Rule
    public MockitoRule mockitoRule = rule();

    @Mock
    private AllureSettingsManager settingsManager;

    @InjectMocks
    private AllureDownloader downloader;

    private String homeDir;

    @Before
    public void setUp() {
        homeDir = Paths.get(FilesUtil.getTempDir().getPath(), "allure-home").toString();
        when(settingsManager.getSettings())
                .thenReturn(new AllureGlobalConfig());
    }

    @Test
    public void itShouldDownloadAndExtractAllureRelease() {
        downloader.downloadAndExtractAllureTo(homeDir, "2.14.0");
        assertTrue(Paths.get(homeDir, "bin", "allure").toFile().exists());
    }

    @After
    public void tearDown() {
        deleteQuietly(Paths.get(homeDir).toFile());
    }
}
