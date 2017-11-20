package io.qameta.allure.bamboo;

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
        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

}
