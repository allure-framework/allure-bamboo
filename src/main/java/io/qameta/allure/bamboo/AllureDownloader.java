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
                final Path extractDir = zipFilePath.getParent();
                ZipUtil.unzip(zipFilePath, extractDir.toString());

                if (homeDir.exists()) {
                    LOGGER.info("Directory {} already exists, removing it..", homeDir);
                    deleteQuietly(homeDir);
                }
                moveDirectory(extractDir.resolve(extractedDirName).toFile(), homeDir);
                return Paths.get(allureHomeDir);
            } catch (ArchiveException | IOException e) {
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
                    final Path downloadToFile = createTempFile("allure", ".zip");
                    LOGGER.info("Downloading allure.zip from {} to {}", url, downloadToFile);
                    return Downloader.download(url, downloadToFile);
                } catch (Exception e) {
                    LOGGER.warn("Failed to download from {}. Root cause : {}.", url, e.toString());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to download Allure of version {}", version, e);
        }
        return Optional.empty();
    }

    private URL[] buildAllureDownloadUrls(final String version) throws MalformedURLException {
        final URL gitUrl = fromPath(settingsManager.getSettings().getDownloadBaseUrl())
                .path(String.format("%s/allure-%s.zip", version, version))
                .build().toURL();
        final URL mavenUrl = fromPath(settingsManager.getSettings().getDownloadCliBaseUrl())
                .path(String.format("allure-commandline/%s/allure-commandline-%s.zip", version, version))
                .build().toURL();
        return new URL[]{gitUrl, mavenUrl};
    }
}
