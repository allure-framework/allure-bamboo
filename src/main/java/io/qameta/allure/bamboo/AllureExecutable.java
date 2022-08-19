package io.qameta.allure.bamboo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.qameta.allure.bamboo.info.AllurePlugins;
import io.qameta.allure.bamboo.util.Downloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.UriBuilder.fromPath;

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
    AllureGenerateResult generate(Collection<Path> sourceDirs, Path targetDir) {
        try {
            final LinkedList<String> args = new LinkedList<>(asList("generate", "-o", targetDir.toString()));
            args.addAll(sourceDirs.stream().map(Path::toString).collect(toList()));
            String output;
            if (cmdLine.isUnix() && cmdLine.hasCommand(BASH_CMD)) {
                args.addFirst(cmdPath.toString());
                output = cmdLine.runCommand("/bin/bash", args.toArray(new String[args.size()]));
            } else {
                output = cmdLine.runCommand(cmdPath.toString(), args.toArray(new String[args.size()]));
            }
            LOGGER.info(output);
            return cmdLine.parseGenerateOutput(output);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate allure report", e);
        }
    }


    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void setCustomLogo(String logoUrl) {
        final String pluginName = "custom-logo-plugin";
        final String allureConfigFileName = "allure.yml";
        final String logoOriginalName = "custom-logo.svg";
        final String logoBackupName = logoOriginalName + "bkp";

        Path rootPath = this.cmdPath.getParent().getParent();
        Path configFolder = rootPath.resolve("config");
        Path logoPluginFolder = rootPath.resolve("plugins").resolve(pluginName).resolve("static");

        /// Editing Yaml to add plugin
        ObjectMapper objectMapper = new YAMLMapper();
        try {
            File configFile = configFolder.resolve(allureConfigFileName).toFile();
            AllurePlugins ap = objectMapper.readValue(configFile, AllurePlugins.class);
            //Saving the file only if it necessary
            if (ap.registerPlugin(pluginName)) {
                objectMapper.writeValue(configFile, ap);
            }
            /// Backup the original one
            URL srcLogoUrl = fromPath(logoUrl).build().toURL();
            File bkp = logoPluginFolder.resolve(logoBackupName).toFile();
            logoPluginFolder.resolve(logoOriginalName).toFile().renameTo(bkp);
            /// Replace with the new one
            Downloader.download(srcLogoUrl, logoPluginFolder.resolve(logoOriginalName));

        } catch (IOException e) {
            LOGGER.error(e.toString());
            throw new RuntimeException(e);
        }
    }

    Path getCmdPath() {
        return cmdPath;
    }
}
