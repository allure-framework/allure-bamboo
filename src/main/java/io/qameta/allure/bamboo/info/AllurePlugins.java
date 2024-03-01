/*
 *  Copyright 2016-2024 Qameta Software Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.qameta.allure.bamboo.info;

import java.util.List;

public class AllurePlugins {
    private List<String> plugins;

    public AllurePlugins(final List<String> plugins) {
        this.plugins = plugins;
    }

    public AllurePlugins() {
        // empty
    }

    public boolean isRegistered(final String pluginName) {
        return plugins.contains(pluginName);
    }

    public List<String> getPlugins() {
        return plugins;
    }

    public boolean registerPlugin(final String pluginName) {
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
