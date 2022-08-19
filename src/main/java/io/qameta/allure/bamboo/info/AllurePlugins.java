package io.qameta.allure.bamboo.info;

import java.util.List;

public class AllurePlugins {
    private List<String> plugins;

    public AllurePlugins(List<String> plugins) {
        this.plugins = plugins;
    }

    public AllurePlugins() {
    }

    public boolean isRegistered(String pluginName) {
        return plugins.contains(pluginName);
    }

    public List<String> getPlugins() {
        return plugins;
    }

    public boolean registerPlugin(String pluginName) {
        if (!this.isRegistered(pluginName)) {
            this.plugins.add(pluginName);
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "\nplugins: " + this.plugins;
    }
}
