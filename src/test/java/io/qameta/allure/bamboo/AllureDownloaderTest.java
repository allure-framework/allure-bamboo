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

import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Paths;

import static io.qameta.allure.bamboo.TestSupport.attachDirectoryTree;
import static io.qameta.allure.bamboo.TestSupport.step;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.junit.MockitoJUnit.rule;

@SuppressWarnings("checkstyle:MultipleStringLiterals")
public class AllureDownloaderTest {

    @Rule
    public MockitoRule mockitoRule = rule();
    @Mock
    private AllureSettingsManager settingsManager;

    private AllureDownloader downloader;
    private HttpServer server;
    private String homeDir;

    @Before
    public void setUp() throws IOException {
        homeDir = Paths.get(System.getProperty("java.io.tmpdir"), "allure-home").toString();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        final byte[] distribution = TestSupport.createAllureDistributionZip("2.17.2");
        server.createContext("/download/2.17.2/allure-2.17.2.zip", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/zip");
            exchange.sendResponseHeaders(200, distribution.length);
            exchange.getResponseBody().write(distribution);
            exchange.close();
        });
        server.start();
        final String baseUrl = String.format("http://127.0.0.1:%s/download/", server.getAddress().getPort());
        final AllureGlobalConfig settings = new AllureGlobalConfig(
                "true",
                "false",
                baseUrl,
                homeDir,
                "false",
                "false"
        );
        when(settingsManager.getSettings()).thenReturn(settings);
        downloader = new AllureDownloader(settingsManager);
    }

    @Test
    public void itShouldDownloadAndExtractAllureRelease() throws Exception {
        step("download and extract the requested Allure distribution", () -> downloader.downloadAndExtractAllureTo(homeDir, "2.17.2"));
        step("verify the extracted home contains the expected CLI layout", () -> {
            attachDirectoryTree("Extracted Allure home", Paths.get(homeDir));
            assertThat(Paths.get(homeDir, "bin", "allure")).exists();
            assertThat(Paths.get(homeDir, "config", "allure.yml")).exists();
            assertThat(Paths.get(homeDir, "plugins", "custom-logo-plugin", "static", "styles.css")).exists();
        });
    }

    @After
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
        deleteQuietly(Paths.get(homeDir).toFile());
    }
}
