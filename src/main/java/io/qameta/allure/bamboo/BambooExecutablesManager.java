package io.qameta.allure.bamboo;

import com.atlassian.bamboo.v2.build.agent.capability.Capability;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityDefaultsHelper;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityImpl;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilitySetManager;
import com.atlassian.bamboo.v2.build.agent.capability.LocalCapabilitySet;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.atlassian.bamboo.v2.build.agent.capability.CapabilitySetManagerUtils.getSharedCapabilitySet;
import static io.qameta.allure.bamboo.AllureConstants.ALLURE_EXECUTION_PREFIX;
import static io.qameta.allure.bamboo.AllureExecutableProvider.DEFAULT_PATH;
import static io.qameta.allure.bamboo.AllureExecutableProvider.DEFAULT_VERSION;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

public class BambooExecutablesManager {
    private final CapabilitySetManager capabilitySetManager;

    public BambooExecutablesManager(CapabilitySetManager capabilitySetManager) {
        this.capabilitySetManager = capabilitySetManager;
    }

    List<String> getAllureExecutables() {
        return getCapabilityKeys().stream()
                .filter(capKey -> capKey.toLowerCase().startsWith(ALLURE_EXECUTION_PREFIX.toLowerCase()))
                .map(this::getCapability)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Capability::getKey)
                .map(cap -> cap.replaceFirst(ALLURE_EXECUTION_PREFIX, ""))
                .collect(toList());
    }

    Optional<String> getDefaultAllureExecutable() {
        return getAllureExecutables().stream().findAny();
    }

    Optional<String> getExecutableByName(String executableName) {
        return getCapabilityKeys().stream()
                .filter(capKey -> {
                    final String[] strings = capKey.split("\\.", 4);
                    return strings.length == 4 && executableName.equals(strings[3]);
                })
                .findFirst()
                .map(this::getCapability)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Capability::getValue);
    }

    void addDefaultAllureExecutableCapability() {
        ofNullable(getSharedCapabilitySet(capabilitySetManager, LocalCapabilitySet.class)).ifPresent(capSet -> {
            final CapabilityImpl capability = new CapabilityImpl(format("%sallure-%s", ALLURE_EXECUTION_PREFIX, DEFAULT_VERSION), DEFAULT_PATH);
            capSet.addCapability(capability);
            capabilitySetManager.saveCapabilitySet(capSet);
        });
    }

    @NotNull
    private Collection<String> getCapabilityKeys() {
        return capabilitySetManager.getSystemCapabilityKeys(CapabilityDefaultsHelper.CAPABILITY_BUILDER_TYPE, false);
    }

    private Optional<Capability> getCapability(String capabilityKey) {
        return ofNullable(capabilitySetManager.getSharedLocalCapabilitySet().getCapability(capabilityKey));
    }
}
