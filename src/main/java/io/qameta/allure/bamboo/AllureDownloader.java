package io.qameta.allure.bamboo;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static java.lang.Integer.getInteger;
import static java.nio.file.Files.createTempFile;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.UriBuilder.fromPath;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.moveDirectory;

class AllureDownloader {
    private static final Logger LOGGER = LoggerFactory.getLogger(AllureDownloader.class);
    private static final int CONN_TIMEOUT_MS = (int) SECONDS.toMillis(getInteger("allure.download.conn.timeout.sec", 10));
    private static final int DOWNLOAD_TIMEOUT_MS = (int) SECONDS.toMillis(getInteger("allure.download.timeout.sec", 60));

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
                final Path extracteDir = zipFilePath.getParent();
                new ZipFile(zipFilePath.toFile()).extractAll(extracteDir.toString());
                if (homeDir.exists()) {
                    LOGGER.info("Directory " + homeDir + " already exists, removing it..");
                    deleteQuietly(homeDir);
                }
                moveDirectory(extracteDir.resolve(extractedDirName).toFile(), homeDir);
                return Paths.get(allureHomeDir);
            } catch (ZipException | IOException e) {
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
                    final URLConnection connection = url.openConnection();
                    connection.setConnectTimeout(CONN_TIMEOUT_MS);
                    connection.setReadTimeout(DOWNLOAD_TIMEOUT_MS);
                    connection.setRequestProperty("Connection", "close");
                    connection.setRequestProperty("Pragma", "no-cache");
                    ((HttpURLConnection) connection).setInstanceFollowRedirects(true);
                    connection.connect();
                    try (InputStream input = connection.getInputStream()) {
                        copyInputStreamToFile(input, downloadToFile.toFile());
                        return Optional.of(downloadToFile);
                    }
                } catch (Exception e) {
                    LOGGER
                        .warn("Failed to download from {}. Root cause : {}. Trying with next url.",
                            url, e.getMessage());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to download Allure of version {}", version, e);
        }
        return Optional.empty();
    }

    private URL[] buildAllureDownloadUrls(String version) throws MalformedURLException {
        URL oldUrl = fromPath(settingsManager.getSettings().getDownloadBaseUrl())
            .path(version + "/" + "allure-" + version + ".zip")
            .build().toURL();
        String binaryName = "allure-commandline";
        URL newUrl = fromPath(settingsManager.getSettings().getDownloadBaseUrl())
            .path(binaryName + "/" + version + "/" + binaryName + "-" + version + ".zip")
            .build().toURL();
        return new URL[]{oldUrl, newUrl};
    }
}
