package io.qameta.allure.bamboo;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.lang.Integer.getInteger;
import static java.nio.file.Files.createTempFile;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.UriBuilder.fromPath;
import static org.apache.commons.io.FileUtils.copyURLToFile;
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

    Optional<Path> downloadAndExtractAllureTo(String home, String version) {
        return downloadAllure(version).map(zipFilePath -> {
            try {
                LOGGER.info("Extracting file {} to {}...", zipFilePath, home);
                final String extractedDirName = "allure-" + version;
                final Path allureHome = Paths.get(home);
                final Path extractDir = zipFilePath.getParent();
                new ZipFile(zipFilePath.toFile()).extractAll(extractDir.toString());
                if (Files.exists(allureHome)) {
                    LOGGER.info("Directory {} already exists, skipping it..", allureHome);
                    return null;
                }
                moveDirectory(extractDir.resolve(extractedDirName).toFile(), allureHome.toFile());
                addExecutablePermissions(allureHome);
                return allureHome;
            } catch (ZipException | IOException e) {
                LOGGER.error("Failed to download and extract Allure of version {} to dir {}", version, home, e);
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
            LOGGER.info("Downloading allure.zip from {} to {}...", url, downloadToFile);
            copyURLToFile(url, downloadToFile.toFile(), CONN_TIMEOUT_MS, DOWNLOAD_TIMEOUT_MS);
            return Optional.of(downloadToFile);
        } catch (IOException e) {
            LOGGER.error("Failed to download Allure of version {}", version, e);
        }
        return Optional.empty();
    }

    private void addExecutablePermissions(Path allureHome) {
        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        Path binaries = allureHome.resolve("bin");
        try {
            Files.setPosixFilePermissions(binaries.resolve("allure"), perms);
            Files.setPosixFilePermissions(binaries.resolve("allure.bat"), perms);
        } catch (IOException e) {
            LOGGER.error("Failed to change Allure bin permissions {}", e);
        }
    }

    private URL buildAllureDownloadUrl(String version) throws MalformedURLException {
        return fromPath(settingsManager.getSettings().getDownloadBaseUrl())
                .path(version).path(String.format("allure-%s.zip", version))
                .build().toURL();
    }
}
