package io.qameta.allure.bamboo;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import org.jetbrains.annotations.NotNull;

/**
 * Executes report generation.
 * Created by bvo2002 on 30.11.16.
 */
public class AllureReportTask implements TaskType {

    public AllureReportTask() {
    }

    @NotNull
    @Override
    public TaskResult execute(@NotNull TaskContext taskContext) {
        final BuildLogger buildLogger = taskContext.getBuildLogger();
        buildLogger.addBuildLogEntry(
                "[Allure Report] This allure report task is now a sham. It does nothing, so please use" +
                        " the suggested way of configuration as listed in the Allure docs! ");
        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

}
