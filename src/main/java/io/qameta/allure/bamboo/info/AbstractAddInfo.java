package io.qameta.allure.bamboo.info;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Abstract class to provide an additional information for reports.
 */
public abstract class AbstractAddInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAddInfo.class);

    public Path invoke(File file) {
        Path outputDirectory = Paths.get(file.toURI());
        try {
            Files.createDirectories(outputDirectory);
        } catch (IOException e) {
            LOGGER.error("Failed to create output directory " + outputDirectory, e);
        }
        Path testRun = outputDirectory.resolve(getFileName());
        try (Writer writer = Files.newBufferedWriter(testRun, StandardCharsets.UTF_8)) {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(writer, getData());
        } catch (IOException e) {
            LOGGER.error("Failed to add executor info into the file " + file.getAbsolutePath(), e);
        }
        return testRun;
    }

    protected abstract Object getData();

    protected abstract String getFileName();

}
