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
package io.qameta.allure.bamboo.util;

import io.qameta.allure.bamboo.AllurePluginException;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;


public final class ZipUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZipUtil.class);
    private static final String DIRECTORY_CREATE_ERROR = "The directory: %s couldn't be created successfully";

    private ZipUtil() {
        // do not instantiate
    }

    public static void unzip(final @NotNull Path zipFilePath,
                             final String outputDir) throws IOException, ArchiveException {

        final ArchiveStreamFactory asf = new ArchiveStreamFactory();

        try (InputStream zipStream = Files.newInputStream(zipFilePath)) {
            try (ArchiveInputStream ais = asf.createArchiveInputStream(ArchiveStreamFactory.ZIP, zipStream)) {
                ArchiveEntry entry = ais.getNextEntry();
                while (entry != null) {
                    final Path entryPath = Paths.get(outputDir, entry.getName());
                    final File entryFile = entryPath.toFile();

                    if (!entry.isDirectory()) {
                        final File parentEntryFile = entryFile.getParentFile();
                        if (parentEntryFile.isDirectory() && !(parentEntryFile.mkdirs() || parentEntryFile.exists())) {
                            throw new IOException(String.format(DIRECTORY_CREATE_ERROR, parentEntryFile.getPath()));
                        }

                        try (OutputStream outputStream = Files.newOutputStream(entryPath)) {
                            IOUtils.copy(ais, outputStream);
                        }
                    } else if (!(entryFile.mkdirs() || entryFile.exists())) {
                        throw new IOException(String.format(DIRECTORY_CREATE_ERROR, entryFile.getPath()));
                    }
                    entry = ais.getNextEntry();
                }
            }
        }
    }

    public static void zipReportFolder(final @NotNull Path srcFolder,
                                       final @NotNull Path targetDir) throws IOException {
        try {
            final Path zipReportTmpDir = Files.createTempDirectory("tmp_allure_report");
            final Path zipReport = zipReportTmpDir.resolve("report.zip");
            try (ZipFile zp = new ZipFile(zipReport.toFile())) {
                zp.addFolder(srcFolder.toFile());
            }
            Files.move(zipReport, targetDir, StandardCopyOption.REPLACE_EXISTING);
            FileUtils.deleteQuietly(zipReportTmpDir.toFile());
        } catch (Exception e) {
            LOGGER.error("Failed to zip allure report", e);
            throw new AllurePluginException("Unexpected error", e);
        }
    }
}
