package ru.yandex.qatools.allure.bamboo;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import org.codehaus.plexus.util.DirectoryScanner;
import org.jetbrains.annotations.NotNull;
import ru.yandex.qatools.allure.report.AllureReportBuilder;
import ru.yandex.qatools.allure.report.AllureReportBuilderException;

import java.io.File;

import static ru.yandex.qatools.allure.bamboo.AllureTaskConstants.GLOB;
import static ru.yandex.qatools.allure.bamboo.AllureTaskConstants.VERSION;

public class AllureTask implements TaskType {
    
    private static final String GLOB_SEPARATOR = ",";
    private static final String RELATIVE_OUTPUT_DIRECTORY = "allure";
    
    @NotNull
    @Override
    public TaskResult execute(@NotNull final TaskContext taskContext) throws TaskException
    {
        final BuildLogger buildLogger = taskContext.getBuildLogger();

        final String glob = taskContext.getConfigurationMap().get(GLOB);
        final String version = taskContext.getConfigurationMap().get(VERSION);
        final File workingDirectory = taskContext.getWorkingDirectory();
        buildLogger.addBuildLogHeader("Allure Task", true);
        buildLogger.addBuildLogEntry("Trying to generate Allure " + version + " using " + workingDirectory.getAbsolutePath() + " as base directory with pattern = " + glob);
        final File[] inputDirectories = findInputDirectoriesByMask(workingDirectory, glob.split(GLOB_SEPARATOR));
        
        if (inputDirectories.length > 0){
            try {
                File allureReportDirectory = new File(workingDirectory, RELATIVE_OUTPUT_DIRECTORY);
                buildLogger.addBuildLogEntry("Allure data will be saved to " + allureReportDirectory.getAbsolutePath());
                AllureReportBuilder builder = new AllureReportBuilder(version, allureReportDirectory);
                buildLogger.addBuildLogEntry("Processing Allure XML files");
                builder.processResults(inputDirectories);
                buildLogger.addBuildLogEntry("Unpacking Allure report face");
                builder.unpackFace();
            } catch (AllureReportBuilderException e) {
                buildLogger.addErrorLogEntry("Caught an exception while generating Allure", e);
                return TaskResultBuilder.newBuilder(taskContext).failedWithError().build();
            }
            buildLogger.addBuildLogEntry("Successfully generated Allure report");
        } else {
            buildLogger.addErrorLogEntry("No input directories found for given base directory and mask");
        }
        
        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

    private static File[] findInputDirectoriesByMask(File baseDir, String[] glob) {
        final DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(baseDir);
        scanner.setIncludes(glob);
        scanner.setCaseSensitive(false);
        scanner.scan();

        final String[] paths = scanner.getIncludedDirectories();
        File[] files = new File[paths.length];
        for (int i = 0; i < paths.length; i++) {
            files[i] = (new File(baseDir, paths[i]));
        }
        return files;
    }

}
