/*
 *  Copyright 2016-2023 Qameta Software OÜ
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

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Map;

import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_ARTIFACT_NAME;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_ENABLED;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_EXECUTABLE;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_FAILED_ONLY;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CUSTOM_LOGO_PATH;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Optional.ofNullable;

public final class AllureBuildConfig implements Serializable {
    private final boolean onlyForFailed;
    private final String executable;
    private final boolean enabled;
    private final String artifactName;
    private final String logoUrl;
    private static final String DEFAULT_ARTIFACT_NAME = "allure-results";
    public static final String DEFAULT_CUSTOM_LOGO_URL = "https://qameta.io/allure-report/img/reportlogo.svg";

    private AllureBuildConfig(final String executable,
                              final String enabled,
                              final String onlyForFailed,
                              final String artifactName,
                              final String logoUrl) {
        this.onlyForFailed = StringUtils.isEmpty(onlyForFailed) ? TRUE : Boolean.parseBoolean(onlyForFailed);
        this.enabled = StringUtils.isEmpty(enabled) ? FALSE : Boolean.parseBoolean(enabled);
        this.executable = executable;
        this.artifactName = artifactName;
        this.logoUrl = !logoUrl.isEmpty() ? logoUrl : AllureBuildConfig.DEFAULT_CUSTOM_LOGO_URL;
    }

    static AllureBuildConfig fromContext(final Map<String, String> context) {
        return new AllureBuildConfig(
                getSingleValue(context, ALLURE_CONFIG_EXECUTABLE, null),
                getSingleValue(context, ALLURE_CONFIG_ENABLED, FALSE.toString()),
                getSingleValue(context, ALLURE_CONFIG_FAILED_ONLY, FALSE.toString()),
                getSingleValue(context, ALLURE_CONFIG_ARTIFACT_NAME, AllureBuildConfig.DEFAULT_ARTIFACT_NAME),
                getSingleValue(context, ALLURE_CUSTOM_LOGO_PATH, AllureBuildConfig.DEFAULT_CUSTOM_LOGO_URL));
    }

    @Nullable
    private static String getSingleValue(final Map context,
                                         final String key,
                                         final String defaultVal) {
        return ofNullable(context.get(key))
                .map(value -> value instanceof String[] ? ((String[]) value)[0] : (String) value)
                .orElse(defaultVal);
    }

    boolean isOnlyForFailed() {
        return onlyForFailed;
    }

    boolean isEnabledSet() {
        return enabled;
    }

    public String getExecutable() {
        return executable;
    }

    boolean isEnabled() {
        return enabled;
    }

    public String getArtifactName() {
        return artifactName;
    }

    public String getCustomLogoUrl() {
        return this.logoUrl;
    }

}
