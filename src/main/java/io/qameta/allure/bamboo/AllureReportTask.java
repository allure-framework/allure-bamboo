package io.qameta.allure.bamboo;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.*;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.bamboo.variable.VariableDefinitionContext;
import com.atlassian.core.util.FileUtils;
import io.qameta.allure.bamboo.info.AddExecutorInfo;
import io.qameta.allure.bamboo.info.AddTestRunInfo;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static io.qameta.allure.bamboo.AllureConstants.*;

/**
 * Executes report generation.
 * Created by bvo2002 on 30.11.16.
 */
public class AllureReportTask implements TaskType {

    private final AllureExecutableProvider allureProvider;
    private CustomVariableContext customVariableContext;

    @Autowired
    public AllureReportTask(final AllureExecutableProvider allureProvider,
                            final CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
        this.allureProvider = allureProvider;
    }

    @NotNull
    @Override
    public TaskResult execute(@NotNull TaskContext taskContext) throws TaskException {
        final BuildLogger buildLogger = taskContext.getBuildLogger();
        final File workingDirectory = taskContext.getWorkingDirectory();
        final AllureReportConfig config = AllureReportConfig.fromContext(taskContext.getConfigurationMap());

        buildLogger.addBuildLogHeader("Allure Report", true);

        Path resultsPath = Paths.get(workingDirectory.getAbsolutePath()).resolve(config.getResultsPath());
        Path reportPath = Paths.get(workingDirectory.getAbsolutePath()).resolve(config.getReportPath());

        buildLogger.addBuildLogEntry(String.format("Results path: %s", resultsPath));
        buildLogger.addBuildLogEntry(String.format("Report path: %s", reportPath));

        try {
            final AllureExecutable allure = allureProvider.provide(true, config.getExecutable()).orElseThrow(
                    () -> new RuntimeException("Failed to find Allure executable by name " + config.getExecutable())
            );
            prepareResults(taskContext);
            allure.generate(resultsPath, reportPath);
        } catch (Exception e) {
            buildLogger.addErrorLogEntry("Caught an exception while generating Allure", e);
            return TaskResultBuilder.newBuilder(taskContext).failedWithError().build();
        }
        buildLogger.addBuildLogEntry("Allure report generated successfully");
        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

    private void prepareResults(TaskContext taskContext) throws IOException, InterruptedException {
        copyHistory(taskContext);
        clearReport(taskContext);
        addTestRunInfo(taskContext);
        addExecutorInfo(taskContext);
    }

    private void copyHistory(TaskContext taskContext) throws IOException {
        Path source = Paths.get(getReportDirectory(taskContext).getAbsolutePath()).resolve("history");
        if (Files.exists(source)) {
            Path destination = Paths.get(getResultDirectory(taskContext).getAbsolutePath()).resolve("history");
            FileUtils.copyDirectory(source.toFile(), destination.toFile(), true);
        }
    }

    private void clearReport(TaskContext taskContext) throws IOException {
        Path reportDirectory = Paths.get(getReportDirectory(taskContext).getAbsolutePath());
        if (Files.exists(reportDirectory)) {
            FileUtils.recursiveDelete(reportDirectory.toFile());
        }
    }

    private void addTestRunInfo(TaskContext taskContext) throws IOException, InterruptedException {
        long start = taskContext.getBuildContext().getBuildResult().getTasksStartDate().getTime();
        long stop = new Date().getTime();
        String buildName = taskContext.getBuildContext().getDisplayName();
        new AddTestRunInfo(buildName, start, stop).invoke(getResultDirectory(taskContext));
    }

    private void addExecutorInfo(TaskContext taskContext) throws IOException, InterruptedException {
        Map<String, VariableDefinitionContext> buildVariables = customVariableContext.getVariableContexts();
        String buildResultsUrl = buildVariables.get("buildResultsUrl").getValue();
        String rootUrl = buildResultsUrl.substring(0, buildResultsUrl.indexOf("bamboo") + "bamboo".length());
        String buildUrl = rootUrl + "/browse/" + buildVariables.get("planKey").getValue() + "-" + taskContext.getBuildContext().getBuildNumber();
        String buildName = taskContext.getBuildContext().getDisplayName();
        String reportUrl = buildUrl + "/artifact/" + buildVariables.get("shortJobKey").getValue() + "/" + ARTIFACT_NAME.replace(" ", "-") + "/index.html";
        new AddExecutorInfo(rootUrl, buildName, buildUrl, reportUrl).invoke(getResultDirectory(taskContext));
    }

    @NotNull
    private File getResultDirectory(TaskContext taskContext) {
        return new File(taskContext.getWorkingDirectory().getAbsolutePath()
                + File.separator + taskContext.getConfigurationMap().get(RESULTS_DIRECTORY));
    }

    @NotNull
    private File getReportDirectory(TaskContext taskContext) {
        return new File(taskContext.getWorkingDirectory().getAbsolutePath()
                + File.separator + taskContext.getConfigurationMap().get(REPORT_DIRECTORY));
    }
}
