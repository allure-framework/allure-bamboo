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

import com.atlassian.bamboo.v2.build.agent.capability.Capability;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityDefaultsHelper;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityImpl;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilitySetManager;
import com.atlassian.bamboo.v2.build.agent.capability.LocalCapabilitySet;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.atlassian.bamboo.v2.build.agent.capability.CapabilitySetManagerUtils.getSharedCapabilitySet;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_EXECUTION_PREFIX;
import static io.qameta.allure.bamboo.AllureExecutableProvider.DEFAULT_PATH;
import static io.qameta.allure.bamboo.AllureExecutableProvider.DEFAULT_VERSION;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public class BambooExecutablesManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(BambooExecutablesManager.class);
    private final CapabilitySetManager capabilitySetManager;

    public BambooExecutablesManager(final CapabilitySetManager capabilitySetManager) {
        this.capabilitySetManager = capabilitySetManager;
    }

    List<String> getAllureExecutables() {
        return getCapabilityKeys().stream()
                .filter(capKey -> capKey.toLowerCase().startsWith(ALLURE_EXECUTION_PREFIX.toLowerCase()))
                .map(this::getCapability)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Capability::getKey)
                .map(cap -> cap.replaceFirst(ALLURE_EXECUTION_PREFIX + ".", ""))
                .collect(toList());
    }

    Optional<String> getDefaultAllureExecutable() {
        return getAllureExecutables().stream().findAny();
    }

    Optional<String> getExecutableByName(final String executableName) {
        LOGGER.debug("Trying to find a capability by executable name '{}'", executableName);
        return getCapabilityKeys().stream()
                .filter(capKey -> {
                    final String[] strings = capKey.split("\\.", 4);
                    final boolean matches = strings.length == 4 && executableName.equals(strings[3]);
                    LOGGER.debug("Checking key '{}' to matches executable name '{}'={}...",
                            capKey, executableName, matches);
                    return matches;
                })
                .findFirst()
                .map(this::getCapability)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Capability::getValue);
    }

    void addDefaultAllureExecutableCapability() {
        Optional.ofNullable(getSharedCapabilitySet(capabilitySetManager, LocalCapabilitySet.class))
                .ifPresent(capSet -> {
                    final String key = format("%s.allure-%s", ALLURE_EXECUTION_PREFIX, DEFAULT_VERSION);
                    final CapabilityImpl capability = new CapabilityImpl(key, DEFAULT_PATH);
                    capSet.addCapability(capability);
                    capabilitySetManager.saveCapabilitySet(capSet);
                });
    }

    @NotNull
    private Collection<String> getCapabilityKeys() {
        return capabilitySetManager
                .getSystemCapabilityKeys(CapabilityDefaultsHelper.CAPABILITY_BUILDER_TYPE, false);
    }

    private Optional<Capability> getCapability(final String capabilityKey) {
        return Optional.ofNullable(
                Objects.requireNonNull(capabilitySetManager.getSharedLocalCapabilitySet())
                        .getCapability(capabilityKey)
        );
    }
}
