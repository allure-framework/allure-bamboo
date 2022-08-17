package io.qameta.allure.bamboo;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.mockito.junit.MockitoJUnit.rule;

public class AllureDownloaderTest {
    @Rule
    public MockitoRule mockitoRule = rule();
    @Mock
    AllureSettingsManager settingsManager;
    AllureGlobalConfig settings;
    @InjectMocks
    AllureDownloader downloader;

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
        File f = Paths.get(homeDir, "bin", "allure").toFile();
        assertTrue(f.exists());
    }

    @After
    public void tearDown() {
        deleteQuietly(Paths.get(homeDir).toFile());
    }
}
