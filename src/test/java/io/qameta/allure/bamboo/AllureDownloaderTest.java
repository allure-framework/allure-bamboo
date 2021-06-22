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
    public void setUp() {
        homeDir = Paths.get(getTempDirectoryPath(), "allure-home").toString();
        settings = new AllureGlobalConfig();
        when(settingsManager.getSettings()).thenReturn(settings);
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