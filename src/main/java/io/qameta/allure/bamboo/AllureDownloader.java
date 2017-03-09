package io.qameta.allure.bamboo;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static java.nio.file.Files.createTempFile;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.io.FileUtils.copyURLToFile;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.moveDirectory;

class AllureDownloader {
    private static final Logger LOGGER = LoggerFactory.getLogger(AllureDownloader.class);
    private static final int CONN_TIMEOUT_MS = (int) SECONDS.toMillis(10);
    private static final int DOWNLOAD_TIMEOUT_MS = (int) SECONDS.toMillis(60);

    Optional<Path> downloadAndExtractAllureTo(String allureHomeDir, String version) {
        return downloadAllure(version).map(zipFilePath -> {
            try {
                LOGGER.info("Extracting file " + zipFilePath + " to " + allureHomeDir + "...");
                final String extractedDirName = "allure-" + version;
                final File homeDir = new File(allureHomeDir);
                final Path extracteDir = zipFilePath.getParent();
                new ZipFile(zipFilePath.toFile()).extractAll(extracteDir.toString());
                if (homeDir.exists()) {
                    LOGGER.info("Directory " + homeDir + " already exists, removing it..");
                    deleteQuietly(homeDir);
                }
                moveDirectory(extracteDir.resolve(extractedDirName).toFile(), homeDir);
                return Paths.get(allureHomeDir);
            } catch (ZipException | IOException e) {
                e.printStackTrace();
                return null;
            } finally {
                deleteQuietly(zipFilePath.toFile());
            }
        });
    }

    private Optional<Path> downloadAllure(String version) {
        try {
            final URL url = buildAllureDownloadUrl(version);
            final Path downloadToFile = createTempFile("allure", "zip");
            LOGGER.info("Downloading allure.zip from " + url + " to " + downloadToFile);
            copyURLToFile(url, downloadToFile.toFile(), CONN_TIMEOUT_MS, DOWNLOAD_TIMEOUT_MS);
            return Optional.of(downloadToFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    private URL buildAllureDownloadUrl(String version) throws MalformedURLException {
        return new URL(
                "https://dl.bintray.com/qameta/generic/io/qameta/allure/allure/" + version + "/allure-" + version + ".zip"
        );
    }
}
