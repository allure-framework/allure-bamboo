package io.qameta.allure.bamboo;

import com.atlassian.bamboo.plan.PlanKeys;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.resultsummary.ResultsSummaryManager;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class AllureViewReportCondition implements Condition {
    private static final Logger LOGGER = LoggerFactory.getLogger(AllureViewReportCondition.class);

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
            } catch (Exception e) {
                LOGGER.error("Failed to evaluate condition", e);
            }
        }
        return false;
    }
}