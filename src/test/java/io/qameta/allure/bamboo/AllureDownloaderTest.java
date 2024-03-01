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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;

import java.io.File;
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
    private AllureGlobalConfig settings;

    @InjectMocks
    private AllureDownloader downloader;

    private String homeDir;

    @Before
    public void setUp() {
        homeDir = Paths.get(System.getProperty("java.io.tmpdir"), "allure-home").toString();
        settings = new AllureGlobalConfig();
        when(settingsManager.getSettings()).thenReturn(settings);
    }

    @Test
    public void itShouldDownloadAndExtractAllureRelease() {
        downloader.downloadAndExtractAllureTo(homeDir, "2.17.2");
        final File f = Paths.get(homeDir, "bin", "allure").toFile();
        assertTrue(f.exists());
    }

    @After
    public void tearDown() {
        deleteQuietly(Paths.get(homeDir).toFile());
    }
}
