package io.qameta.allure.bamboo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Paths;

import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.getTempDirectoryPath;
import static org.junit.Assert.assertTrue;

public class AllureDownloaderTest {

    private String homeDir;

    @Before
    public void setUp() throws Exception {
        homeDir = Paths.get(getTempDirectoryPath(), "allure-home").toString();
    }

    @Test
    public void itShouldDownloadAndExtractAllure() throws Exception {
        AllureDownloader downloader = new AllureDownloader();

        downloader.downloadAndExtractAllureTo(homeDir, "2.0-BETA5");

        assertTrue(Paths.get(homeDir, "bin", "allure").toFile().exists());
    }

    @After
    public void tearDown() throws Exception {
        deleteQuietly(Paths.get(homeDir).toFile());
    }
}