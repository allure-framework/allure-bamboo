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
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_DOWNLOAD_CLI_URL;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_DOWNLOAD_ENABLED;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_DOWNLOAD_URL;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_ENABLED_BY_DEFAULT;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_LOCAL_STORAGE;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CUSTOM_LOGO_ENABLED;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Boolean.parseBoolean;
import static org.apache.commons.lang3.SystemUtils.getJavaIoTmpDir;

class AllureGlobalConfig implements Serializable {
    private static final String DEFAULT_DOWNLOAD_BASE_URL
            = "https://github.com/allure-framework/allure2/releases/download/";
    private static final String DEFAULT_CLI_BASE_URL
            = "https://repo.maven.apache.org/maven2/io/qameta/allure/";
    public static final String DEFAULT_LOCAL_STORAGE_PATH = new File(getJavaIoTmpDir(), "allure-reports").getPath();
    private final boolean downloadEnabled;
    private final boolean customLogoEnabled;
    private final boolean enabledByDefault;
    private final String localStoragePath;
    private final String downloadBaseUrl;
    private final String downloadCliBaseUrl;

    AllureGlobalConfig() {
        this(TRUE.toString(), FALSE.toString(),
                DEFAULT_DOWNLOAD_BASE_URL, DEFAULT_LOCAL_STORAGE_PATH,
                DEFAULT_CLI_BASE_URL, TRUE.toString());
    }

    AllureGlobalConfig(final String downloadEnabled,
                       final String enabledByDefault,
                       final String downloadBaseUrl,
                       final String localStoragePath,
                       final String cmdLineUrl,
                       final String customLogoEnable) {
        this.downloadEnabled = StringUtils.isEmpty(downloadEnabled) ? TRUE : parseBoolean(downloadEnabled);
        this.enabledByDefault = StringUtils.isEmpty(enabledByDefault) ? FALSE : parseBoolean(enabledByDefault);
        this.downloadBaseUrl = StringUtils.isEmpty(downloadBaseUrl) ? DEFAULT_DOWNLOAD_BASE_URL : downloadBaseUrl;
        this.downloadCliBaseUrl = StringUtils.isEmpty(cmdLineUrl) ? DEFAULT_CLI_BASE_URL : cmdLineUrl;
        this.localStoragePath = StringUtils.isEmpty(localStoragePath) ? DEFAULT_LOCAL_STORAGE_PATH : localStoragePath;
        this.customLogoEnabled = StringUtils.isEmpty(customLogoEnable) ? TRUE : parseBoolean(customLogoEnable);
    }


    @NotNull
    static AllureGlobalConfig fromContext(final Map context) {
        return new AllureGlobalConfig(
                getSingleValue(context, ALLURE_CONFIG_DOWNLOAD_ENABLED, FALSE.toString()),
                getSingleValue(context, ALLURE_CONFIG_ENABLED_BY_DEFAULT, FALSE.toString()),
                getSingleValue(context, ALLURE_CONFIG_DOWNLOAD_URL, DEFAULT_DOWNLOAD_BASE_URL),
                getSingleValue(context, ALLURE_CONFIG_LOCAL_STORAGE, DEFAULT_LOCAL_STORAGE_PATH),
                getSingleValue(context, ALLURE_CONFIG_DOWNLOAD_CLI_URL, DEFAULT_CLI_BASE_URL),
                getSingleValue(context, ALLURE_CUSTOM_LOGO_ENABLED, FALSE.toString())
        );
    }

    @Nullable
    private static String getSingleValue(final Map context,
                                         final String key,
                                         final String defaultVal) {
        return Optional.ofNullable(context.get(key))
                .map(value -> value instanceof String[] ? ((String[]) value)[0] : (String) value)
                .orElse(defaultVal);
    }

    void toContext(final @NotNull Map<String, Object> context) {
        context.put(ALLURE_CONFIG_DOWNLOAD_ENABLED, isDownloadEnabled());
        context.put(ALLURE_CONFIG_ENABLED_BY_DEFAULT, isEnabledByDefault());
        context.put(ALLURE_CONFIG_DOWNLOAD_URL, getDownloadBaseUrl());
        context.put(ALLURE_CONFIG_DOWNLOAD_CLI_URL, getDownloadCliBaseUrl());
        context.put(ALLURE_CONFIG_LOCAL_STORAGE, getLocalStoragePath());
        context.put(ALLURE_CUSTOM_LOGO_ENABLED, isCustomLogoEnabled());
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
