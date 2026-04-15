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

import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.resultsummary.ResultsSummaryManager;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;

import java.util.HashMap;
import java.util.Map;

import static com.atlassian.bamboo.plan.PlanKeys.getPlanResultKey;
import static io.qameta.allure.bamboo.AllureBuildResult.allureBuildResult;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.junit.MockitoJUnit.rule;

@SuppressWarnings("checkstyle:MultipleStringLiterals")
public class AllureViewReportConditionTest {

    private static final String PLAN_KEY = "PROJ-PLAN";

    @Rule
    public MockitoRule mockitoRule = rule();

    @Mock
    private ResultsSummaryManager resultsSummaryManager;

    @Mock
    private ResultsSummary resultsSummary;

    @Test
    public void itShouldDisplayWhenResultFinishedAndAllureInfoIsPresent() {
        final AllureViewReportCondition condition = new AllureViewReportCondition(resultsSummaryManager);
        final Map<String, Object> context = new HashMap<>();
        context.put("buildKey", PLAN_KEY);
        context.put("buildNumber", "5");
        when(resultsSummaryManager.getResultsSummary(getPlanResultKey(PLAN_KEY, 5))).thenReturn(resultsSummary);
        when(resultsSummary.isFinished()).thenReturn(true);
        when(resultsSummary.isNotBuilt()).thenReturn(false);
        when(resultsSummary.getCustomBuildData()).thenReturn(customData("handler", null));

        assertTrue(condition.shouldDisplay(context));
    }

    @Test
    public void itShouldUsePlanKeyWhenBuildKeyIsMissing() {
        final AllureViewReportCondition condition = new AllureViewReportCondition(resultsSummaryManager);
        final Map<String, Object> context = new HashMap<>();
        context.put("planKey", PLAN_KEY);
        context.put("buildNumber", "7");
        when(resultsSummaryManager.getResultsSummary(getPlanResultKey(PLAN_KEY, 7))).thenReturn(resultsSummary);
        when(resultsSummary.isFinished()).thenReturn(false);
        when(resultsSummary.isNotBuilt()).thenReturn(true);
        when(resultsSummary.getCustomBuildData()).thenReturn(customData("handler", null));

        assertTrue(condition.shouldDisplay(context));
    }

    @Test
    public void itShouldHideWhenBuildContextIsIncomplete() {
        final AllureViewReportCondition condition = new AllureViewReportCondition(resultsSummaryManager);
        final Map<String, Object> context = new HashMap<>();
        context.put("planKey", PLAN_KEY);

        assertFalse(condition.shouldDisplay(context));
    }

    @Test
    public void itShouldHideWhenResultIsNotFinishedYet() {
        final AllureViewReportCondition condition = new AllureViewReportCondition(resultsSummaryManager);
        final Map<String, Object> context = new HashMap<>();
        context.put("buildKey", PLAN_KEY);
        context.put("buildNumber", "3");
        when(resultsSummaryManager.getResultsSummary(getPlanResultKey(PLAN_KEY, 3))).thenReturn(resultsSummary);
        when(resultsSummary.isFinished()).thenReturn(false);
        when(resultsSummary.isNotBuilt()).thenReturn(false);
        when(resultsSummary.getCustomBuildData()).thenReturn(customData("handler", null));

        assertFalse(condition.shouldDisplay(context));
    }

    @Test
    public void itShouldHideWhenBuildNumberIsInvalid() {
        final AllureViewReportCondition condition = new AllureViewReportCondition(resultsSummaryManager);
        final Map<String, Object> context = new HashMap<>();
        context.put("buildKey", PLAN_KEY);
        context.put("buildNumber", "not-a-number");

        assertFalse(condition.shouldDisplay(context));
    }

    private Map<String, String> customData(final String handlerClass,
                                           final String details) {
        final Map<String, String> customData = new HashMap<>();
        allureBuildResult(true, details).withHandlerClass(handlerClass).dumpToCustomData(customData);
        return customData;
    }
}
