package io.qameta.allure.bamboo.info;

import net.sf.json.JSONObject;

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

    public Path invoke(File file) throws IOException, InterruptedException {
        Path outputDirectory = Paths.get(file.toURI());
        Files.createDirectories(outputDirectory);
        Path testRun = outputDirectory.resolve(getFileName());
        try (Writer writer = Files.newBufferedWriter(testRun, StandardCharsets.UTF_8)) {
            JSONObject.fromObject(getData())
                    .write(writer)
                    .flush();
        }
        return testRun;
    }

    protected abstract Object getData();

    protected abstract String getFileName();

}
