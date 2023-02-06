package io.qameta.allure.bamboo.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

public class FileStringReplacer {

    public static void replaceInFile(Path filePath, String oldString, String newString) throws IOException {
        String content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
        content = content.replaceAll(oldString, newString);
        Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
    }

    public static void replaceInFile(Path filePath, @NotNull Pattern pattern, String newString) throws IOException {
        String content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
        content = pattern.matcher(content).replaceAll(newString);
        Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
    }

}
