package io.qameta.allure.bamboo;

import com.atlassian.sal.api.message.Message;
import com.atlassian.sal.api.upgrade.PluginUpgradeTask;

import java.util.Collection;
import java.util.Collections;

public class AllurePluginInstallTask implements PluginUpgradeTask {
    private final BambooExecutablesManager executablesManager;

    public AllurePluginInstallTask(BambooExecutablesManager executablesManager) {
        this.executablesManager = executablesManager;
    }

    @Override
    public int getBuildNumber() {
        return 1;
    }

    @Override
    public String getShortDescription() {
        return "Installs Allure Plugin first time";
    }

    @Override
    public Collection<Message> doUpgrade() throws Exception {
        if (executablesManager.getAllureExecutables().isEmpty()) {
            executablesManager.addDefaultAllureExecutableCapability();
        }
        return Collections.emptySet();
    }

    @Override
    public String getPluginKey() {
        return "io.qameta.allure.allure-bamboo";
    }
}
