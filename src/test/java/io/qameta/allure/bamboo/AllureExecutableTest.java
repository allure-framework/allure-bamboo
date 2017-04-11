package io.qameta.allure.bamboo;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.google.common.io.Files.createTempDir;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.junit.MockitoJUnit.rule;

public class AllureExecutableTest {
    @Rule
    public MockitoRule mockitoRule = rule();

    private Path path = Paths.get("/tmp/where-allure/installed");

    @Mock
    private AllureCommandLineSupport cmdLine;

    private AllureExecutable executable;
    private Path fromDir;
    private Path toDir;

    @Before
    public void setUp() throws Exception {
        executable = new AllureExecutable(path, cmdLine);
        fromDir = createTempDir().toPath();
        toDir = createTempDir().toPath();
        when(cmdLine.parseGenerateOutput(anyString())).thenReturn(new AllureGenerateResult("", true));
    }

    @Test
    public void itShouldInvokeAllureGenerateOnUnixWithBash() throws Exception {
        when(cmdLine.hasCommand("/bin/bash")).thenReturn(true);
        when(cmdLine.isUnix()).thenReturn(true);

        executable.generate(fromDir, toDir);

        verify(cmdLine).runCommand("/bin/bash", path.toString(), "generate", "-o", toDir.toString(), "-v", fromDir.toString());
    }

    @Test
    public void itShouldInvokeAllureGenerateOnUnixWithoutBash() throws Exception {
        when(cmdLine.hasCommand("/bin/bash")).thenReturn(false);
        when(cmdLine.isUnix()).thenReturn(true);

        executable.generate(fromDir, toDir);

        verify(cmdLine).runCommand(path.toString(), "generate", "-o", toDir.toString(), "-v", fromDir.toString());
    }

    @Test
    public void itShouldInvokeAllureGenerateOnWindows() throws Exception {
        when(cmdLine.isUnix()).thenReturn(false);

        executable.generate(fromDir, toDir);

        verify(cmdLine).runCommand(path.toString(), "generate", "-o", toDir.toString(), "-v", fromDir.toString());

    }
}