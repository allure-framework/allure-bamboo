package io.qameta.allure.bamboo;

import io.qameta.allure.bamboo.config.AllureGlobalConfig;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;

import java.util.Optional;

import static io.qameta.allure.bamboo.AllureExecutableProvider.DEFAULT_VERSION;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.junit.MockitoJUnit.rule;

public class AllureExecutableProviderTest {

    private final String homeDir = "/home/allure";
    @Rule
    public MockitoRule mockitoRule = rule();
    @Mock
    private BambooExecutablesManager executablesManager;
    @Mock
    private AllureDownloader downloader;
    @InjectMocks
    private AllureExecutableProvider provider;
    private AllureGlobalConfig config = new AllureGlobalConfig(true, true);

    @Test
    public void itShouldProvideDefaultVersion() throws Exception {
        provide("Allure WITHOUT VERSION");
        verify(downloader).downloadAndExtractAllureTo(homeDir, DEFAULT_VERSION);
    }

    @Test
    public void itShouldProvideTheGivenVersionWithoutMilestone() throws Exception {
        provide("Allure 2.0");
        verify(downloader).downloadAndExtractAllureTo(homeDir, "2.0");
    }

    @Test
    public void itShouldProvideTheGivenVersionWithMilestone() throws Exception {
        provide("Allure 2.0-BETA4");
        verify(downloader).downloadAndExtractAllureTo(homeDir, "2.0-BETA4");
    }

    private void provide(String executableName) {
        when(executablesManager.getExecutableByName(executableName)).thenReturn(Optional.of(homeDir));
        provider.provide(config, executableName);
    }
}