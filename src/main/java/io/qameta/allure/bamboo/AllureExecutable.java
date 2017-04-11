package io.qameta.allure.bamboo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;

import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.forceMkdir;

class AllureExecutable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AllureExecutable.class);
    private static final String BASH_CMD = "/bin/bash";
    private final Path cmdPath;
    private final AllureCommandLineSupport cmdLine;

    AllureExecutable(Path cmdPath, AllureCommandLineSupport commandLine) {
        this.cmdPath = cmdPath;
        this.cmdLine = commandLine;
    }

    @Nonnull
    AllureGenerateResult generate(Path sourceDir, Path targetDir) {
        try {
            forceMkdir(targetDir.toFile());
            final LinkedList<String> args = new LinkedList<>(asList("generate", "-o", targetDir.toString(), "-v", sourceDir.toString()));
            String output;
            if (cmdLine.isUnix() && cmdLine.hasCommand(BASH_CMD)) {
                args.addFirst(cmdPath.toString());
                output = cmdLine.runCommand("/bin/bash", args.toArray(new String[args.size()]));
            } else {
                output = cmdLine.runCommand(cmdPath.toString(), args.toArray(new String[args.size()]));
            }
            LOGGER.info(output);
            return cmdLine.parseGenerateOutput(output);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate allure report", e);
        }
    }

    Path getCmdPath() {
        return cmdPath;
    }
}
