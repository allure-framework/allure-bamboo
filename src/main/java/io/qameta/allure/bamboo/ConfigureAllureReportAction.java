package io.qameta.allure.bamboo;

import com.atlassian.bamboo.configuration.GlobalAdminAction;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.Preparable;
import io.qameta.allure.bamboo.config.AllureGlobalConfig;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.atlassian.bamboo.util.ActionParamsUtils.getStringArrayMap;
import static java.util.stream.Collectors.toList;
import static org.apache.logging.log4j.util.Strings.isEmpty;

public class ConfigureAllureReportAction extends GlobalAdminAction implements Preparable {

    private final AllureSettingsManager settingsManager;
    private AllureGlobalConfig config;

    public ConfigureAllureReportAction(AllureSettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    @Override
    public String execute() throws Exception {
        settingsManager.saveSettings(AllureGlobalConfig.fromContext(getStringArrayMap()));
        return super.execute();
    }

    @Override
    public void validate() {
        super.validate();
        final Map<String, String[]> valuesMap = getStringArrayMap();
        final AllureGlobalConfig config = AllureGlobalConfig.fromContext(valuesMap);
        if (isEmpty(config.getStoragePath())) {
            addActionError(getText("allure.config.storage.path.error.required"));
        }
        if (!valuesMap.containsKey(AllureConstants.ALLURE_CONFIG_STORAGE_TYPE)) {
            addActionError(getText("allure.config.storage.path.error.required"));
        }
    }

    private AllureGlobalConfig getAllureConfig() {
        return settingsManager.getSettings();
    }

    @Override
    public void prepare() throws Exception {
        this.config = getAllureConfig();
        getAllureConfig().toContext(ActionContext.getContext().getContextMap());
    }

    public List<String> getStorageOptions() {
        return Stream.of(AllureGlobalConfig.StorageType.values()).map(AllureGlobalConfig.StorageType::name).collect(toList());
    }

    public AllureGlobalConfig getConfig() {
        return config;
    }
}
