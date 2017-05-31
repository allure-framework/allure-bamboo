package io.qameta.allure.bamboo;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;

import java.nio.file.Paths;

import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.getTempDirectoryPath;
import static org.junit.Assert.assertTrue;
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
    public void setUp() throws Exception {
        homeDir = Paths.get(getTempDirectoryPath(), "allure-home").toString();
        settings = new AllureGlobalConfig();
        when(settingsManager.getSettings()).thenReturn(settings);
    }

    @Test
    public void itShouldDownloadAndExtractAllureBeta() throws Exception {
        downloader.downloadAndExtractAllureTo(homeDir, "2.0-BETA5");

        assertTrue(Paths.get(homeDir, "bin", "allure").toFile().exists());
        assertTrue(Paths.get(homeDir, "bin", "allure").toFile().canExecute());
        assertTrue(Paths.get(homeDir, "bin", "allure.bat").toFile().canExecute());
    }

    @Test
    public void itShouldDownloadAndExtractAllureRelease() throws Exception {
        downloader.downloadAndExtractAllureTo(homeDir, "2.0.1");

        assertTrue(Paths.get(homeDir, "bin", "allure").toFile().exists());
        assertTrue(Paths.get(homeDir, "bin", "allure").toFile().canExecute());
        assertTrue(Paths.get(homeDir, "bin", "allure.bat").toFile().canExecute());
    }

    @After
    public void tearDown() throws Exception {
        deleteQuietly(Paths.get(homeDir).toFile());
    }
}