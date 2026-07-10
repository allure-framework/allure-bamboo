/*
 *  Copyright 2016-2026 Qameta Software Inc
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

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.qameta.allure.Allure;
import io.qameta.allure.bamboo.info.allurewidgets.summary.Statistic;
import io.qameta.allure.bamboo.info.allurewidgets.summary.Summary;
import io.qameta.allure.bamboo.info.allurewidgets.summary.Time;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@SuppressWarnings(
    {
            "checkstyle:ClassDataAbstractionCoupling",
            "checkstyle:MultipleStringLiterals"
    }
)
public final class TestSupport {

    private TestSupport() {
        // utility class
    }

    static byte[] createZip(final Map<String, String> entries) throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            final Set<String> createdDirectories = new HashSet<>();
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                addDirectories(zipOutputStream, createdDirectories, entry.getKey());
                zipOutputStream.putNextEntry(new ZipEntry(entry.getKey()));
                zipOutputStream.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zipOutputStream.closeEntry();
            }
        }
        return outputStream.toByteArray();
    }

    static byte[] createAllureDistributionZip(final String version) throws IOException {
        final Map<String, String> entries = new LinkedHashMap<>();
        entries.put("allure-" + version + "/bin/allure", "#!/bin/sh\necho allure\n");
        entries.put("allure-" + version + "/config/allure.yml", "plugins: []\n");
        entries.put(
                "allure-" + version + "/plugins/custom-logo-plugin/static/styles.css",
                ".side-nav__brand { background: url(default); left: 10px !important; }\n"
        );
        return createZip(entries);
    }

    static void writeMinimalReport(final Path reportDir) throws IOException {
        Files.createDirectories(reportDir.resolve("widgets"));
        new JsonMapper().writeValue(
                reportDir.resolve("widgets").resolve("summary.json").toFile(),
                new Summary(
                        "Original",
                        Collections.emptyList(),
                        new Statistic(0, 0, 0, 1, 0, 1),
                        new Time(1L, 2L, 1, 1, 1, 1)
                )
        );
        Files.writeString(reportDir.resolve("app.js"), "window.tpl='>Allure</span>';\n", StandardCharsets.UTF_8);
        Files.writeString(
                reportDir.resolve("index.html"),
                "<html><head><title>Original</title></head><body>report</body></html>\n",
                StandardCharsets.UTF_8
        );
    }

    public static void step(final String name,
                            final ThrowingRunnable action)
            throws Exception {
        Allure.step(name, action::run);
    }

    public static <T> T step(final String name,
                             final ThrowingSupplier<T> action)
            throws Exception {
        final Object[] value = new Object[1];
        Allure.step(name, () -> value[0] = action.get());
        @SuppressWarnings("unchecked")
        final T typedValue = (T) value[0];
        return typedValue;
    }

    public static void attachText(final String name,
                                  final String value) {
        Allure.addAttachment(name, "text/plain", value, ".txt");
    }

    static void attachDirectoryTree(final String name,
                                    final Path root)
            throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            final String listing = paths
                    .map(path -> root.equals(path) ? "." : root.relativize(path).toString())
                    .sorted()
                    .collect(Collectors.joining(System.lineSeparator()));
            attachText(name, listing);
        }
    }

    static ServletOutputStream servletOutputStream(final ByteArrayOutputStream outputStream) {
        return new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(final WriteListener writeListener) {
                // no-op
            }

            @Override
            public void write(final int b) {
                outputStream.write(b);
            }
        };
    }

    private static void addDirectories(final ZipOutputStream zipOutputStream,
                                       final Set<String> createdDirectories,
                                       final String entryName)
            throws IOException {
        int separatorIndex = entryName.indexOf('/');
        while (separatorIndex > 0) {
            final String directoryName = entryName.substring(0, separatorIndex + 1);
            if (createdDirectories.add(directoryName)) {
                zipOutputStream.putNextEntry(new ZipEntry(directoryName));
                zipOutputStream.closeEntry();
            }
            separatorIndex = entryName.indexOf('/', separatorIndex + 1);
        }
    }

    @FunctionalInterface
    public interface ThrowingRunnable {

        void run() throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {

        T get() throws Exception;
    }
}
