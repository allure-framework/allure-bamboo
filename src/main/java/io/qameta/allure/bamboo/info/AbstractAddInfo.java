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
package io.qameta.allure.bamboo.info;

import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Abstract class to provide an additional information for reports.
 */
public abstract class AbstractAddInfo implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAddInfo.class);

    public Path invoke(final File file) {
        final Path outputDirectory = Paths.get(file.toURI());
        try {
            Files.createDirectories(outputDirectory);
        } catch (IOException e) {
            LOGGER.error("Failed to create output directory " + outputDirectory, e);
        }
        final Path testRun = outputDirectory.resolve(getFileName());
        try (Writer writer = Files.newBufferedWriter(testRun, StandardCharsets.UTF_8)) {
            JSONObject.fromObject(getData())
                    .write(writer)
                    .flush();
        } catch (IOException e) {
            LOGGER.error("Failed to add executor info into the file " + file.getAbsolutePath(), e);
        }
        return testRun;
    }

    protected abstract Object getData();

    protected abstract String getFileName();

}
