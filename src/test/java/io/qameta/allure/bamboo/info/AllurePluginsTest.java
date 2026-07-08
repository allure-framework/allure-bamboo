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

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.Test;

import static io.qameta.allure.bamboo.TestSupport.attachText;
import static io.qameta.allure.bamboo.TestSupport.step;
import static org.assertj.core.api.Assertions.assertThat;

public class AllurePluginsTest {

    private static final String PLUGIN = "custom-logo-plugin";

    @Test
    public void itShouldRegisterPluginOnFreshInstanceWithoutNpe() throws Exception {
        final AllurePlugins plugins = new AllurePlugins();

        final boolean added = plugins.registerPlugin(PLUGIN);

        step("verify a fresh instance registered the plugin without a NullPointerException", () -> {
            attachText("Registered plugins", String.valueOf(plugins.getPlugins()));
            assertThat(added).isTrue();
            assertThat(plugins.isRegistered(PLUGIN)).isTrue();
            assertThat(plugins.getPlugins()).containsExactly(PLUGIN);
        });
    }

    @Test
    public void itShouldExposeEmptyListWhenConfigHasNoPluginsKey() throws Exception {
        // Mirrors setCustomLogo reading an allure.yml distribution config that omits `plugins:`.
        final AllurePlugins plugins = new YAMLMapper().readValue("{}", AllurePlugins.class);

        step("verify a config without a plugins key yields a non-null, mutable list", () -> {
            attachText("Parsed plugins", String.valueOf(plugins.getPlugins()));
            assertThat(plugins.getPlugins()).isNotNull().isEmpty();
            assertThat(plugins.registerPlugin(PLUGIN)).isTrue();
            assertThat(plugins.getPlugins()).containsExactly(PLUGIN);
        });
    }
}
