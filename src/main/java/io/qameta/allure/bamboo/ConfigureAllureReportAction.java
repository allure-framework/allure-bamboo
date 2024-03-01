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
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.Preparable;

import java.util.Map;

import static com.atlassian.bamboo.util.ActionParamsUtils.getStringArrayMap;

public class ConfigureAllureReportAction extends GlobalAdminAction implements Preparable {

    private final transient AllureSettingsManager settingsManager;
    private AllureGlobalConfig config;

    public ConfigureAllureReportAction(final AllureSettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    @Override
    public String execute() throws Exception {
        settingsManager.saveSettings(AllureGlobalConfig.fromContext(getStringArrayMap()));
        return super.execute();
    }

    @Override
    public void validate() {
        super.validate();
        final Map<String, String[]> valuesMap = getStringArrayMap();
        if (!valuesMap.containsKey(AllureConstants.ALLURE_CONFIG_DOWNLOAD_URL)) {
            addActionError(getText("allure.config.download.url.error.required"));
        }
        if (!valuesMap.containsKey(AllureConstants.ALLURE_CONFIG_LOCAL_STORAGE)) {
            addActionError(getText("allure.config.download.local.storage.required"));
        }
    }

    private AllureGlobalConfig getAllureConfig() {
        return settingsManager.getSettings();
    }

    @Override
    public void prepare() throws Exception {
        this.config = getAllureConfig();
        getAllureConfig().toContext(ActionContext.getContext().getContextMap());
    }

    public AllureGlobalConfig getConfig() {
        return config;
    }
}
