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

import java.util.HashMap;

/**
 * Add executor info to reports.
 */
public class AddExecutorInfo extends AbstractAddInfo {

    private static final long serialVersionUID = 1L;

    private static final String EXECUTOR_JSON = "executor.json";

    private final String url;

    private final String buildId;

    private final String buildUrl;

    private final String buildName;

    private final String reportUrl;

    public AddExecutorInfo(final String url,
                           final String buildId,
                           final String buildName,
                           final String buildUrl,
                           final String reportUrl) {
        this.url = url;
        this.buildId = buildId;
        this.buildUrl = buildUrl;
        this.buildName = buildName;
        this.reportUrl = reportUrl;
    }

    @Override
    protected Object getData() {
        final HashMap<String, Object> data = new HashMap<>();
        data.put("name", "Bamboo");
        data.put("type", "bamboo");
        data.put("url", url);
        data.put("buildOrder", buildId);
        data.put("buildName", buildName);
        data.put("buildUrl", buildUrl);
        data.put("reportUrl", reportUrl);
        return data;
    }

    @Override
    protected String getFileName() {
        return EXECUTOR_JSON;
    }
}
