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

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.Serializable;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Boolean.parseBoolean;
import static org.apache.commons.lang3.SystemUtils.getJavaIoTmpDir;

public class AllureGlobalConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_DOWNLOAD_BASE_URL
            = "https://github.com/allure-framework/allure2/releases/download/";
    private static final String DEFAULT_CLI_BASE_URL
            = "https://repo.maven.apache.org/maven2/io/qameta/allure/";
    public static final String DEFAULT_LOCAL_STORAGE_PATH
            = new File(getJavaIoTmpDir(), "allure-reports").getPath();
    private final boolean downloadEnabled;
    private final boolean customLogoEnabled;
    private final boolean enabledByDefault;
    private final boolean enabledReportsCleanup;
    private final String localStoragePath;
    private final String downloadBaseUrl;
    private final String downloadCliBaseUrl;

    public AllureGlobalConfig() {
        this(TRUE.toString(),
                FALSE.toString(),
                DEFAULT_DOWNLOAD_BASE_URL,
                DEFAULT_LOCAL_STORAGE_PATH,
                DEFAULT_CLI_BASE_URL,
                TRUE.toString(),
                FALSE.toString());
    }

    public AllureGlobalConfig(final String downloadEnabled,
                              final String enabledByDefault,
                              final String downloadBaseUrl,
                              final String localStoragePath,
                              final String cmdLineUrl,
                              final String customLogoEnable,
                              final String enabledReportsCleanup) {
        this.downloadEnabled = StringUtils.isBlank(downloadEnabled)
                ? TRUE : parseBoolean(downloadEnabled);
        this.enabledByDefault = StringUtils.isBlank(enabledByDefault)
                ? FALSE : parseBoolean(enabledByDefault);
        this.downloadBaseUrl = StringUtils.isBlank(downloadBaseUrl)
                ? DEFAULT_DOWNLOAD_BASE_URL : downloadBaseUrl;
        this.downloadCliBaseUrl = StringUtils.isBlank(cmdLineUrl)
                ? DEFAULT_CLI_BASE_URL : cmdLineUrl;
        this.localStoragePath = StringUtils.isBlank(localStoragePath)
                ? DEFAULT_LOCAL_STORAGE_PATH : localStoragePath;
        this.customLogoEnabled = StringUtils.isBlank(customLogoEnable)
                ? TRUE : parseBoolean(customLogoEnable);
        this.enabledReportsCleanup = StringUtils.isBlank(enabledReportsCleanup)
                ? FALSE : parseBoolean(enabledReportsCleanup);
    }

    public AllureGlobalConfig(final boolean downloadEnabled,
                              final boolean enabledByDefault,
                              final String downloadBaseUrl,
                              final String localStoragePath,
                              final boolean customLogoEnable,
                              final boolean enabledReportsCleanup) {
        this.downloadEnabled = downloadEnabled;
        this.enabledByDefault = enabledByDefault;
        this.downloadBaseUrl = StringUtils.isBlank(downloadBaseUrl)
                ? DEFAULT_DOWNLOAD_BASE_URL : downloadBaseUrl;
        this.downloadCliBaseUrl = DEFAULT_CLI_BASE_URL;
        this.localStoragePath = StringUtils.isBlank(localStoragePath)
                ? DEFAULT_LOCAL_STORAGE_PATH : localStoragePath;
        this.customLogoEnabled = customLogoEnable;
        this.enabledReportsCleanup = enabledReportsCleanup;
    }

    boolean isDownloadEnabled() {
        return downloadEnabled;
    }

    boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    boolean isCustomLogoEnabled() {
        return customLogoEnabled;
    }

    boolean isEnabledReportsCleanup() {
        return enabledReportsCleanup;
    }

    String getDownloadBaseUrl() {
        return downloadBaseUrl;
    }

    String getDownloadCliBaseUrl() {
        return downloadCliBaseUrl;
    }

    public String getLocalStoragePath() {
        return localStoragePath;
    }
}
