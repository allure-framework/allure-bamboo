package io.qameta.allure.bamboo;

import com.atlassian.bamboo.plan.PlanKeys;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.resultsummary.ResultsSummaryManager;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import io.qameta.allure.bamboo.info.AllureBuildResult;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class AllureViewReportCondition implements Condition {

    private final ResultsSummaryManager resultsSummaryManager;

    public AllureViewReportCondition(ResultsSummaryManager resultsSummaryManager) {
        this.resultsSummaryManager = resultsSummaryManager;
    }

    @Override
    public void init(Map<String, String> context) throws PluginParseException {
    }

    @Override
    public boolean shouldDisplay(Map<String, Object> context) {
        final String buildKey = StringUtils.defaultString((String) context.get("planKey"), (String) context.get("buildKey"));
        final String buildNumberString = (String) context.get("buildNumber");
        if (buildKey != null && buildNumberString != null) {
            try {
                int buildNumber = Integer.parseInt(buildNumberString);
                final ResultsSummary resultsSummary = this.resultsSummaryManager.getResultsSummary(PlanKeys.getPlanResultKey(buildKey, buildNumber));
                if (resultsSummary != null) {
                    final AllureBuildResult buildResult = AllureBuildResult.fromCustomData(resultsSummary.getCustomBuildData());
                    return (buildResult.hasInfo() && (resultsSummary.isFinished() || resultsSummary.isNotBuilt()));
                }
            } catch (Exception ignored) {
            }
        }

        return false;
    }
}