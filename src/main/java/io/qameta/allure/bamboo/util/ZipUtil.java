package io.qameta.allure.bamboo.util;

import net.lingala.zip4j.ZipFile;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.move;

public class ZipUtil {

    public static void unzip(@NotNull Path zipFilePath, String outputDir) throws IOException, ArchiveException {

        final ArchiveStreamFactory asf = new ArchiveStreamFactory();

        InputStream zipStream = Files.newInputStream(zipFilePath);
        ArchiveInputStream ais = asf.createArchiveInputStream(ArchiveStreamFactory.ZIP, zipStream);

        for (ArchiveEntry entry; (entry = ais.getNextEntry()) != null; ) {
            Path entryPath = Paths.get(outputDir, entry.getName());
            File entryFile = entryPath.toFile();

            if (!entry.isDirectory()) {
                File parentEntryFile = entryFile.getParentFile();
                if (parentEntryFile.isDirectory() && !(parentEntryFile.mkdirs() || parentEntryFile.exists())) {
                    throw new IOException("The directory: " + parentEntryFile.getPath() + " couldn't be created successfully");
                }

                try (OutputStream outputStream = Files.newOutputStream(entryPath)) {
                    IOUtils.copy(ais, outputStream);
                }
            } else if (!(entryFile.mkdirs() || entryFile.exists())) {
                throw new IOException("The directory: " + entryFile.getPath() + " couldn't be created successfully");
            }
        }
    }

    public static void zipFolder(@NotNull Path srcFolder, @NotNull Path targetDir) throws IOException {
        Path zipReportTmpDir = createTempDirectory("tmp_allure_report");
        Path zipReport = zipReportTmpDir.resolve("report.zip");
        try (ZipFile zp = new ZipFile(zipReport.toFile())) {
            zp.addFolder(srcFolder.toFile());
        }
        move(zipReport, targetDir, StandardCopyOption.REPLACE_EXISTING);
    }
}
