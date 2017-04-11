package io.qameta.allure.bamboo;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;
import static org.apache.commons.lang3.SystemUtils.IS_OS_UNIX;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.buildobjects.process.ProcBuilder.run;

public class AllureCommandLineSupport {
    private static final Pattern RESULT_TC_COUNT_REGEX = Pattern.compile(".+Found (\\d+) test cases.+", Pattern.DOTALL);

    String runCommand(String cmd, String... args) {
        return run(cmd, args);
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
        return Paths.get(command).toFile().exists();
    }
}
