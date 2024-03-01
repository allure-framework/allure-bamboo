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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import static java.lang.Integer.getInteger;
import static java.util.concurrent.TimeUnit.SECONDS;

public final class Downloader {

    private static final int CONN_TIMEOUT_MS = (int) SECONDS.toMillis(
            getInteger("allure.download.conn.timeout.sec", 20));
    private static final int DOWNLOAD_TIMEOUT_MS = (int) SECONDS.toMillis(
            getInteger("allure.download.timeout.sec", 120));

    private Downloader() {
        // do not instantiate
    }

    public static Optional<Path> download(final URL url,
                                          final Path target) throws IOException {
        final URLConnection connection = url.openConnection();
        connection.setConnectTimeout(CONN_TIMEOUT_MS);
        connection.setReadTimeout(DOWNLOAD_TIMEOUT_MS);
        connection.setRequestProperty("Connection", "close");
        connection.setRequestProperty("Pragma", "no-cache");
        ((HttpURLConnection) connection).setInstanceFollowRedirects(true);
        connection.connect();
        try (InputStream input = connection.getInputStream()) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            return Optional.of(target);
        }
    }
}
