package io.qameta.allure.bamboo;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.process.ExternalProcessBuilder;
import com.atlassian.bamboo.process.ProcessService;
import com.atlassian.bamboo.task.*;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.bamboo.variable.VariableDefinitionContext;
import com.atlassian.core.util.FileUtils;
import com.atlassian.utils.process.ExternalProcess;
import com.google.common.base.Preconditions;
import io.qameta.allure.bamboo.callables.AddExecutorInfo;
import io.qameta.allure.bamboo.callables.AddTestRunInfo;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static io.qameta.allure.bamboo.AllureConstants.*;

/**
 * Executes report generation.
 * Created by bvo2002 on 30.11.16.
 */
public class AllureTask implements TaskType {

    private final ProcessService processService;
    private final EnvironmentVariableAccessor environmentVariableAccessor;
    private final CapabilityContext capabilityContext;
    private final AllureCapabilityDefaultsHelper helper;

    private CustomVariableContext customVariableContext;

    @Autowired
    public AllureTask(ProcessService processService,
                      EnvironmentVariableAccessor environmentVariableAccessor,
                      CapabilityContext capabilityContext, CustomVariableContext customVariableContext) {
        this.processService = processService;
        this.environmentVariableAccessor = environmentVariableAccessor;
        this.capabilityContext = capabilityContext;
        this.helper = new AllureCapabilityDefaultsHelper();
        this.customVariableContext = customVariableContext;
    }

    @NotNull
    @Override
    public TaskResult execute(@NotNull TaskContext taskContext) throws TaskException {

        BuildLogger buildLogger = taskContext.getBuildLogger();
        TaskResultBuilder taskResultBuilder = TaskResultBuilder.newBuilder(taskContext);
        Map<String, String> environment = this.environmentVariableAccessor.splitEnvironmentAssignments(taskContext.getConfigurationMap().get("environmentVariables"), false);
        final File workingDirectory = taskContext.getWorkingDirectory();

        buildLogger.addBuildLogHeader("Allure Task", true);
        buildLogger.addBuildLogEntry("Trying to generate Allure using " + workingDirectory.getAbsolutePath() + " as base directory with pattern = " + taskContext.getConfigurationMap().get(RESULTS_DIRECTORY));
        buildLogger.addBuildLogEntry("Allure data will be saved to " + workingDirectory.getAbsolutePath() + File.separator + taskContext.getConfigurationMap().get(REPORT_PATH_PREFIX));
        try {
            prepareResults(taskContext);

            ExternalProcessBuilder e = new ExternalProcessBuilder();
            e.workingDirectory(taskContext.getWorkingDirectory());
            e.env(environment);
            e.command(this.getCommandList(taskContext));
            ExternalProcess externalProcess = this.processService.executeExternalProcess(taskContext, e);
            taskResultBuilder.checkReturnCode(externalProcess);
        } catch (Exception e) {
            buildLogger.addErrorLogEntry("Caught an exception while generating Allure", e);
            return TaskResultBuilder.newBuilder(taskContext).failedWithError().build();
        }
        buildLogger.addBuildLogEntry("Successfully generated Allure report");
        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

    private void prepareResults(TaskContext taskContext) throws IOException, InterruptedException {
        copyHistory(taskContext);
        clearReport(taskContext);
        addTestRunInfo(taskContext);
        addExecutorInfo(taskContext);
    }

    private void copyHistory(TaskContext taskContext) throws IOException {
        Path source = Paths.get(getReportDirectory(taskContext).getAbsolutePath() + "/data/history.json");
        if (Files.exists(source)) {
            Path destination = Paths.get(getResultDirectory(taskContext).getAbsolutePath() + "/history.json");
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
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
        new AddTestRunInfo(taskContext.getBuildContext().getBuildResultKey(), start, stop).invoke(getResultDirectory(taskContext));
    }

    private void addExecutorInfo(TaskContext taskContext) throws IOException, InterruptedException {
        Map<String, VariableDefinitionContext> buildVariables = customVariableContext.getVariableContexts();
        String buildResultsUrl = buildVariables.get("buildResultsUrl").getValue();
        String rootUrl = buildResultsUrl.substring(0, buildResultsUrl.indexOf("bamboo")+"bamboo".length());
        String buildUrl = rootUrl + "/browse/" + buildVariables.get("planKey").getValue() + "-" + taskContext.getBuildContext().getBuildNumber();
        String reportUrl = buildUrl + "/artifact/" + buildVariables.get("shortJobKey").getValue() + "/" + ARTIFACT_NAME.replace(" ", "-") + "/index.html";
        new AddExecutorInfo(rootUrl, taskContext.getBuildContext().getBuildResultKey(), buildUrl, reportUrl).invoke(getResultDirectory(taskContext));
    }

    @NotNull
    private File getResultDirectory(TaskContext taskContext) {
        return new File(taskContext.getWorkingDirectory().getAbsolutePath()
                + File.separator + taskContext.getConfigurationMap().get(RESULTS_DIRECTORY));
    }

    @NotNull
    private File getReportDirectory(TaskContext taskContext) {
        return new File(taskContext.getWorkingDirectory().getAbsolutePath()
                + File.separator + taskContext.getConfigurationMap().get(REPORT_PATH_PREFIX));
    }

    private List<String> getCommandList(TaskContext taskContext) throws TaskException {
        List<String> list = new ArrayList<>();
        list.add(this.getExecutable(taskContext));
        list.add("generate");
        list.add(taskContext.getConfigurationMap().get(RESULTS_DIRECTORY));
        list.add("-o");
        list.add(taskContext.getConfigurationMap().get(REPORT_PATH_PREFIX));
        return list;
    }

    private String getExecutable(TaskContext taskContext) {

        BuildLogger buildLogger = taskContext.getBuildLogger();

        String path = this.getCapabilityPath(taskContext);
        String executableName = this.helper.getExecutableName();

        String executableFile = path + File.separatorChar + "bin" + File.separatorChar + executableName;
        File file = new File(executableFile);
        if (!file.isAbsolute())
            file = new File(taskContext.getWorkingDirectory(), executableFile);

        buildLogger.addBuildLogEntry("Allure executable file: " + executableFile);
        if (!file.exists())
            throw new IllegalStateException("Cannot find executable \'" + executableFile + "\'");
        return file.getAbsolutePath();
    }

    @NotNull
    private String getCapabilityPath(CommonTaskContext taskContext) {
        String builderLabel = Preconditions.checkNotNull(taskContext.getConfigurationMap().get(EXECUTABLE_LABEL),
                "Executable label is not defined");
        return Preconditions.checkNotNull(this.capabilityContext.getCapabilityValue(AllureCapabilityDefaultsHelper.ALLURE_CAPABILITY_PREFIX + "." + builderLabel),
                "Executable path is not defined");
    }
}
