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

import com.atlassian.bamboo.plan.PlanKeys;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.resultsummary.ResultsSummaryManager;
import com.atlassian.plugin.web.Condition;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class AllureViewReportCondition implements Condition {
    private static final Logger LOGGER = LoggerFactory.getLogger(AllureViewReportCondition.class);

    private final ResultsSummaryManager resultsSummaryManager;

    public AllureViewReportCondition(final ResultsSummaryManager resultsSummaryManager) {
        this.resultsSummaryManager = resultsSummaryManager;
    }

    @Override
    public void init(final Map<String, String> context) {
        // does nothing
    }

    @Override
    public boolean shouldDisplay(final Map<String, Object> context) {
        final String buildKey = StringUtils.defaultString(
                (String) context.get("planKey"),
                (String) context.get("buildKey")
        );
        final String buildNumberString = (String) context.get("buildNumber");
        if (buildKey != null && buildNumberString != null) {
            try {
                final int buildNumber = Integer.parseInt(buildNumberString);
                final ResultsSummary resultsSummary = this.resultsSummaryManager
                        .getResultsSummary(PlanKeys.getPlanResultKey(buildKey, buildNumber));
                if (resultsSummary != null) {
                    final AllureBuildResult buildResult = AllureBuildResult
                            .fromCustomData(resultsSummary.getCustomBuildData());
                    return buildResult.hasInfo() && (resultsSummary.isFinished() || resultsSummary.isNotBuilt());
                }
            } catch (Exception e) {
                LOGGER.error("Failed to evaluate condition", e);
            }
        }
        return false;
    }
}
