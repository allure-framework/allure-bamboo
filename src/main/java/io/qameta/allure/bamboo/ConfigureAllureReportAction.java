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

import com.atlassian.bamboo.configuration.GlobalAdminAction;
import com.atlassian.struts.Preparable;
import org.apache.commons.lang3.StringUtils;

public class ConfigureAllureReportAction extends GlobalAdminAction implements Preparable {

    private final transient AllureSettingsManager settingsManager;
    private AllureGlobalConfig config;

    private boolean downloadEnabled;
    private boolean customLogoEnabled;
    private boolean enabledByDefault;
    private boolean enabledReportsCleanup;
    private String localStoragePath;
    private String downloadBaseUrl;

    public ConfigureAllureReportAction(final AllureSettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    @Override
    public String execute() {
        final AllureGlobalConfig newConfig = new AllureGlobalConfig(
                downloadEnabled,
                enabledByDefault,
                downloadBaseUrl,
                localStoragePath,
                customLogoEnabled,
                enabledReportsCleanup
        );
        settingsManager.saveSettings(newConfig);
        this.config = settingsManager.getSettings();
        return SUCCESS;
    }

    @Override
    public String input() {
        this.downloadEnabled = this.config.isDownloadEnabled();
        this.customLogoEnabled = this.config.isCustomLogoEnabled();
        this.enabledByDefault = this.config.isEnabledByDefault();
        this.enabledReportsCleanup = this.config.isEnabledReportsCleanup();
        this.localStoragePath = this.config.getLocalStoragePath();
        this.downloadBaseUrl = this.config.getDownloadBaseUrl();
        return INPUT;
    }

    @Override
    public void validate() {
        if (StringUtils.isBlank(downloadBaseUrl)) {
            addActionError(getText("allure.config.download.url.error.required"));
        }
        if (StringUtils.isBlank(localStoragePath)) {
            addActionError(getText("allure.config.local.storage.required"));
        }
    }

    @Override
    public void prepare() {
        this.config = settingsManager.getSettings();
    }

    public boolean isDownloadEnabled() {
        return downloadEnabled;
    }

    public void setDownloadEnabled(final boolean downloadEnabled) {
        this.downloadEnabled = downloadEnabled;
    }

    public boolean isCustomLogoEnabled() {
        return customLogoEnabled;
    }

    public void setCustomLogoEnabled(final boolean customLogoEnabled) {
        this.customLogoEnabled = customLogoEnabled;
    }

    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    public void setEnabledByDefault(final boolean enabledByDefault) {
        this.enabledByDefault = enabledByDefault;
    }

    public boolean isEnabledReportsCleanup() {
        return enabledReportsCleanup;
    }

    public void setEnabledReportsCleanup(final boolean enabledReportsCleanup) {
        this.enabledReportsCleanup = enabledReportsCleanup;
    }

    public String getLocalStoragePath() {
        return localStoragePath;
    }

    public void setLocalStoragePath(final String localStoragePath) {
        this.localStoragePath = localStoragePath;
    }

    public String getDownloadBaseUrl() {
        return downloadBaseUrl;
    }

    public void setDownloadBaseUrl(final String downloadBaseUrl) {
        this.downloadBaseUrl = downloadBaseUrl;
    }
}
