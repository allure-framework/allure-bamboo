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

import java.io.Serializable;
import java.util.Map;

import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_ARTIFACT_NAME;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_ENABLED;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_EXECUTABLE;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_CONFIG_FAILED_ONLY;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.apache.commons.lang.StringUtils.isEmpty;

public final class AllureBuildConfig implements Serializable {

    private final boolean onlyForFailed;
    private final String executable;
    private final boolean enabled;
    private final String artifactName;

    private AllureBuildConfig(final String executable,
                              final String enabled,
                              final String onlyForFailed,
                              final String artifactName) {
        this.onlyForFailed = isEmpty(onlyForFailed) ? TRUE : Boolean.parseBoolean(onlyForFailed);
        this.enabled = isEmpty(enabled) ? FALSE : Boolean.parseBoolean(enabled);
        this.executable = executable;
        this.artifactName = artifactName;
    }

    static AllureBuildConfig fromContext(final Map<String, String> context) {
        final String failedOnlyString = context.get(ALLURE_CONFIG_FAILED_ONLY);
        final String enableAllureString = context.get(ALLURE_CONFIG_ENABLED);
        return new AllureBuildConfig(
                context.get(ALLURE_CONFIG_EXECUTABLE),
                enableAllureString,
                failedOnlyString,
                context.get(ALLURE_CONFIG_ARTIFACT_NAME));
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
}
