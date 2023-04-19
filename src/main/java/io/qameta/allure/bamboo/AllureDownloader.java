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
import net.lingala.zip4j.ZipFile;
import org.apache.commons.io.FileUtils;
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
import java.util.concurrent.TimeUnit;

import static java.lang.Integer.getInteger;
import static javax.ws.rs.core.UriBuilder.fromPath;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.moveDirectory;

class AllureDownloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureDownloader.class);
    private static final long CONN_TIMEOUT_MS = TimeUnit.SECONDS
            .toMillis(getInteger("allure.download.conn.timeout.sec", 10));
    private static final long DOWNLOAD_TIMEOUT_MS = TimeUnit.SECONDS
            .toMillis(getInteger("allure.download.timeout.sec", 60));

    private final AllureSettingsManager settingsManager;

    AllureDownloader(final AllureSettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    Optional<Path> downloadAndExtractAllureTo(final String allureHomeDir,
                                              final String version) {
        return downloadAllure(version).map(zipFilePath -> {
            try {
                LOGGER.info("Extracting file {} to {}...", zipFilePath, allureHomeDir);
                final String extractedDirName = "allure-" + version;
                final File homeDir = new File(allureHomeDir);
                final Path extractedDir = zipFilePath.getParent();
                try (ZipFile zf = new ZipFile(zipFilePath.toFile())) {
                    zf.extractAll(extractedDir.toString());
                }
                if (homeDir.exists()) {
                    LOGGER.info("Directory " + homeDir + " already exists, removing it..");
                    deleteQuietly(homeDir);
                }
                moveDirectory(extractedDir.resolve(extractedDirName).toFile(), homeDir);
                return Paths.get(allureHomeDir);
            } catch (IOException e) {
                LOGGER.error("Failed to download and extract Allure of version {} to dir {}",
                        version, allureHomeDir, e);
                return null;
            } finally {
                deleteQuietly(zipFilePath.toFile());
            }
        });
    }

    private Optional<Path> downloadAllure(final String version) {
        try {
            final URL[] urls = buildAllureDownloadUrls(version);
            for (URL url : urls) {
                try {
                    final Path downloadToFile = FilesUtil.createTempFile("allure", ".zip");
                    LOGGER.info("Downloading allure.zip from {} to {}", url, downloadToFile);
                    final URLConnection connection = url.openConnection();
                    connection.setConnectTimeout((int) CONN_TIMEOUT_MS);
                    connection.setReadTimeout((int) DOWNLOAD_TIMEOUT_MS);
                    connection.setRequestProperty("Connection", "close");
                    connection.setRequestProperty("Pragma", "no-cache");
                    ((HttpURLConnection) connection).setInstanceFollowRedirects(true);
                    connection.connect();
                    try (InputStream input = connection.getInputStream()) {
                        FileUtils.copyInputStreamToFile(input, downloadToFile.toFile());
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

    private URL[] buildAllureDownloadUrls(final String version) throws MalformedURLException {
        final URL oldUrl = fromPath(settingsManager.getSettings().getDownloadBaseUrl())
            .path(String.format("%s/allure-%s.zip", version, version))
            .build().toURL();
        final String binaryName = "allure-commandline";
        final URL newUrl = fromPath(settingsManager.getSettings().getDownloadBaseUrl())
            .path(String.format("%s/%s/%s-%s.zip", binaryName, version, binaryName, version))
            .build().toURL();
        return new URL[]{oldUrl, newUrl};
    }
}
