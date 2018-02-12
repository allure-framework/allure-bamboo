package io.qameta.allure.bamboo;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static io.qameta.allure.bamboo.AllureExecutableProvider.DEFAULT_VERSION;
import static io.qameta.allure.bamboo.AllureExecutableProvider.getAllureSubDir;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.junit.MockitoJUnit.rule;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class AllureExecutableProviderTest {

    private final String homeDir = "/home/allure";
    private final String binaryDir = Paths.get(this.homeDir, getAllureSubDir()).toString();
    @Rule
    public MockitoRule mockitoRule = rule();
    @Mock
    private BambooExecutablesManager executablesManager;
    @Mock
    private AllureDownloader downloader;
    @Mock
    private AllureCommandLineSupport cmdLine;
    @InjectMocks
    private AllureExecutableProvider provider;
    private AllureGlobalConfig config;
    private Path allureCmdPath;
    private Path allureBatCmdPath;

    @Before
    public void setUp() throws Exception {
        config = new AllureGlobalConfig();
        allureCmdPath = Paths.get(binaryDir, "bin", "allure");
        allureBatCmdPath = Paths.get(binaryDir, "bin", "allure.bat");
        when(downloader.downloadAndExtractAllureTo(anyString(), anyString())).thenReturn(Optional.empty());
    }

    @Test
    public void itShouldProvideDefaultVersion() throws Exception {
        provide("Allure WITHOUT VERSION");
        verify(downloader).downloadAndExtractAllureTo(binaryDir, DEFAULT_VERSION);
    }

    @Test
    public void itShouldProvideTheGivenVersionWithFullSemverWithoutName() throws Exception {
        provide("2.0.0");
        verify(downloader).downloadAndExtractAllureTo(binaryDir, "2.0.0");
    }

    @Test
    public void itShouldProvideTheGivenVersionWithFullSemverWithoutMilestone() throws Exception {
        provide("Allure 2.0.0");
        verify(downloader).downloadAndExtractAllureTo(binaryDir, "2.0.0");
    }

    @Test
    public void itShouldProvideTheGivenVersionWithMajorMinorWithoutMilestone() throws Exception {
        provide("Allure 2.0");
        verify(downloader).downloadAndExtractAllureTo(binaryDir, "2.0");
    }

    @Test
    public void itShouldProvideTheGivenVersionWithMilestone() throws Exception {
        provide("Allure 2.0-BETA4");
        verify(downloader).downloadAndExtractAllureTo(binaryDir, "2.0-BETA4");
    }

    @Test
    public void itShouldProvideExecutableForUnix() throws Exception {
        when(cmdLine.hasCommand(allureCmdPath.toString())).thenReturn(true);
        when(cmdLine.isWindows()).thenReturn(false);

        final Optional<AllureExecutable> res = provide("Allure 2.0-BETA5");

        assertThat(res.isPresent(), equalTo(true));
        assertThat(res.get().getCmdPath(), equalTo(allureCmdPath));
    }

    @Test
    public void itShouldProvideExecutableForWindows() throws Exception {
        when(cmdLine.hasCommand(allureBatCmdPath.toString())).thenReturn(true);
        when(cmdLine.isWindows()).thenReturn(true);

        final Optional<AllureExecutable> res = provide("Allure 2.0.0");

        assertThat(res.isPresent(), equalTo(true));
        assertThat(res.get().getCmdPath(), equalTo(allureBatCmdPath));
    }

    private Optional<AllureExecutable> provide(String executableName) {
        when(executablesManager.getExecutableByName(executableName)).thenReturn(Optional.of(homeDir));
        return provider.provide(config, executableName);
    }
}