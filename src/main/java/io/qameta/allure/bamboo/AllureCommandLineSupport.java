package io.qameta.allure.bamboo;

import org.buildobjects.process.ProcBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.apache.commons.lang3.SystemUtils.IS_OS_UNIX;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;

public class AllureCommandLineSupport {
    private static final Pattern RESULT_TC_COUNT_REGEX = Pattern.compile(".+Found (\\d+) test cases.+", Pattern.DOTALL);
    private static final int GENERATE_TIMEOUT_MS = (int) MINUTES.toMillis(10);

    String runCommand(String cmd, String... args) {
        return new ProcBuilder(cmd)
                .withArgs(args).withTimeoutMillis(GENERATE_TIMEOUT_MS)
                .run().getOutputString();
    }

    @NotNull
    AllureGenerateResult parseGenerateOutput(String output) {
        boolean success = true;
        final Matcher matcher = RESULT_TC_COUNT_REGEX.matcher(output);
        if (matcher.matches()) {
            success = parseInt(matcher.group(1)) > 0;
        }
        return new AllureGenerateResult(output, success);
    }

    boolean isUnix() {
        return IS_OS_UNIX;
    }

    boolean isWindows() {
        return IS_OS_WINDOWS;
    }

    boolean hasCommand(String command) {
        File cmdFile = Paths.get(command).toFile();
        // It needs to be sure that the command is a file
        return cmdFile.exists() && cmdFile.isFile();
    }
}
