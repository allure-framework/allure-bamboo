package io.qameta.allure.bamboo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;

import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.lang3.SystemUtils.IS_OS_LINUX;
import static org.buildobjects.process.ProcBuilder.run;

class AllureExecutable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AllureExecutable.class);
    private final Path cmdPath;

    AllureExecutable(Path cmdPath) {
        this.cmdPath = cmdPath;
    }

    String generate(Path sourceDir, Path targetDir) {
        try {
            forceMkdir(targetDir.toFile());
            final LinkedList<String> args = new LinkedList<>(asList("generate", "-o", targetDir.toString(), "-v", sourceDir.toString()));
            String output;
            if (IS_OS_LINUX && Paths.get("/bin/bash").toFile().exists()) {
                args.addFirst(cmdPath.toString());
                output = run("/bin/bash", args.toArray(new String[args.size()]));
            } else {
                output = run(cmdPath.toString(), args.toArray(new String[args.size()]));
            }
            LOGGER.info(output);
            return output;
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate allure report", e);
        }
    }
}
