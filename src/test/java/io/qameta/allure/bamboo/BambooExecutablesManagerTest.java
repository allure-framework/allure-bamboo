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
package io.qameta.allure.bamboo;

import com.atlassian.bamboo.v2.build.agent.capability.CapabilityDefaultsHelper;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityImpl;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilitySetManager;
import com.atlassian.bamboo.v2.build.agent.capability.LocalCapabilitySet;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;

import java.util.List;

import static io.qameta.allure.bamboo.AllureConstants.ALLURE_EXECUTION_PREFIX;
import static io.qameta.allure.bamboo.AllureExecutableProvider.DEFAULT_PATH;
import static io.qameta.allure.bamboo.AllureExecutableProvider.DEFAULT_VERSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.junit.MockitoJUnit.rule;

@SuppressWarnings("checkstyle:MultipleStringLiterals")
public class BambooExecutablesManagerTest {

    @Rule
    public MockitoRule mockitoRule = rule();

    @Mock
    private CapabilitySetManager capabilitySetManager;

    @Test
    public void itShouldListConfiguredAllureExecutables() {
        final String capabilityKey = ALLURE_EXECUTION_PREFIX + ".allure-2.30.0";
        final LocalCapabilitySet capabilitySet = new LocalCapabilitySet();
        capabilitySet.addCapability(new CapabilityImpl(capabilityKey, "/opt/allure"));
        when(capabilitySetManager.getSystemCapabilityKeys(CapabilityDefaultsHelper.CAPABILITY_BUILDER_TYPE, false))
                .thenReturn(List.of(capabilityKey, "system.builder.maven"));
        when(capabilitySetManager.getSharedLocalCapabilitySet()).thenReturn(capabilitySet);

        final BambooExecutablesManager manager = new BambooExecutablesManager(capabilitySetManager);

        assertThat(manager.getAllureExecutables()).containsExactly("allure-2.30.0");
        assertThat(manager.getExecutableByName("allure-2.30.0")).hasValue("/opt/allure");
    }

    @Test
    public void itShouldAddTheDefaultCapabilityToTheSharedLocalSet() {
        final LocalCapabilitySet capabilitySet = new LocalCapabilitySet();
        when(capabilitySetManager.getSharedLocalCapabilitySet()).thenReturn(capabilitySet);

        final BambooExecutablesManager manager = new BambooExecutablesManager(capabilitySetManager);
        manager.addDefaultAllureExecutableCapability();

        assertThat(capabilitySet.getCapability(ALLURE_EXECUTION_PREFIX + ".allure-" + DEFAULT_VERSION)).isNotNull();
        assertThat(capabilitySet.getCapability(ALLURE_EXECUTION_PREFIX + ".allure-" + DEFAULT_VERSION).getValue())
                .isEqualTo(DEFAULT_PATH);
        verify(capabilitySetManager).saveCapabilitySet(capabilitySet);
    }
}
