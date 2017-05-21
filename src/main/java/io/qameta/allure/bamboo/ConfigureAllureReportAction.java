package io.qameta.allure.bamboo;

import com.atlassian.bamboo.configuration.GlobalAdminAction;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.Preparable;

import java.util.Map;

import static com.atlassian.bamboo.util.ActionParamsUtils.getStringArrayMap;

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
        if (!valuesMap.containsKey(AllureConstants.ALLURE_CONFIG_DOWNLOAD_URL)) {
            addActionError(getText("allure.config.download.url.error.required"));
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

    public AllureGlobalConfig getConfig() {
        return config;
    }
}
