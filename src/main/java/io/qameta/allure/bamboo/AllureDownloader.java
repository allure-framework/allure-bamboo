package io.qameta.allure.bamboo;

import io.qameta.allure.bamboo.util.Downloader;
import io.qameta.allure.bamboo.util.ZipUtil;
import org.apache.commons.compress.archivers.ArchiveException;
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
import static javax.ws.rs.core.UriBuilder.fromPath;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.moveDirectory;

class AllureDownloader {
    private static final Logger LOGGER = LoggerFactory.getLogger(AllureDownloader.class);

    private final AllureSettingsManager settingsManager;

    AllureDownloader(AllureSettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    Optional<Path> downloadAndExtractAllureTo(String allureHomeDir, String version) {
        return downloadAllure(version).map(zipFilePath -> {
            try {
                LOGGER.info("Extracting file {} to {}...", zipFilePath, allureHomeDir);
                final String extractedDirName = "allure-" + version;
                final File homeDir = new File(allureHomeDir);
                final Path extractDir = zipFilePath.getParent();
                ZipUtil.unzip(zipFilePath, extractDir.toString());

                if (homeDir.exists()) {
                    LOGGER.info("Directory " + homeDir + " already exists, removing it..");
                    deleteQuietly(homeDir);
                }
                moveDirectory(extractDir.resolve(extractedDirName).toFile(), homeDir);
                return Paths.get(allureHomeDir);
            } catch (ArchiveException | IOException e) {
                LOGGER.error("Failed to download and extract Allure of version {} to dir {}", version, allureHomeDir, e);
                return null;
            } finally {
                deleteQuietly(zipFilePath.toFile());
            }
        });
    }

    private Optional<Path> downloadAllure(String version) {
        try {
            URL[] urls = buildAllureDownloadUrls(version);
            for (URL url : urls) {
                try {
                    final Path downloadToFile = createTempFile("allure", ".zip");
                    LOGGER.info("Downloading allure.zip from {} to {}", url, downloadToFile);
                    return Downloader.download(url, downloadToFile);
                } catch (Exception e) {
                    LOGGER
                        .warn("Failed to download from {}. Root cause : {}.",
                            url, e.toString());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to download Allure of version {}", version, e);
        }
        return Optional.empty();
    }

    private URL[] buildAllureDownloadUrls(String version) throws MalformedURLException {
        URL gitUrl = fromPath(settingsManager.getSettings().getDownloadBaseUrl())
            .path(version + "/" + "allure-" + version + ".zip")
            .build().toURL();
        String binaryName = "allure-commandline";
        URL mavenUrl = fromPath(settingsManager.getSettings().getDownloadCliBaseUrl())
            .path(binaryName + "/" + version + "/" + binaryName + "-" + version + ".zip")
            .build().toURL();
        return new URL[]{gitUrl, mavenUrl};
    }
}
