package io.qameta.allure.bamboo;

import com.atlassian.bamboo.v2.build.agent.capability.AbstractHomeDirectoryCapabilityDefaultsHelper;
import com.atlassian.bamboo.v2.build.agent.capability.Capability;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityDefaultsHelper;
import com.atlassian.bamboo.v2.build.agent.capability.ExecutablePathUtils;
import org.apache.commons.lang.WordUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * Define the default capability for allure.
 * Created by bvo2002 on 30.11.16.
 */
public class AllureCapabilityDefaultsHelper extends AbstractHomeDirectoryCapabilityDefaultsHelper {

    private static final String ALLURE_HOME = "ALLURE_HOME";
    private static final String ALLURE_HOME_POSIX = "/usr/share/allure/";
    private static final String ALLURE_EXEC_NAME = "allure";
    static final String ALLURE_CAPABILITY_PREFIX = CapabilityDefaultsHelper.CAPABILITY_BUILDER_PREFIX + "." + ALLURE_EXEC_NAME;

    AllureCapabilityDefaultsHelper() {
        super();
    }

    @Nullable
    @Override
    protected Capability createCapability(@NotNull File content) {
        return super.createCapability(content);
    }

    @NotNull
    @Override
    protected String getExecutableName() {
        return  ExecutablePathUtils.makeBatchIfOnWindows(ALLURE_EXEC_NAME);
    }

    @Override
    protected String getEnvHome() { return System.getenv(ALLURE_HOME); }

    @Override
    protected String getPosixHome() {
        return ALLURE_HOME_POSIX;
    }

    @NotNull
    @Override
    protected String getCapabilityKey() { return ALLURE_CAPABILITY_PREFIX + "." + WordUtils.capitalize(ALLURE_EXEC_NAME); }
}
