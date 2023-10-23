[@ui.bambooSection titleKey="allure.config.title"]
    [@ww.checkbox labelKey='allure.config.enable.checkbox.label' name='custom.allure.config.enabled' toggle='true' /]

    [@ww.checkbox labelKey='custom.allure.config.failed.only.label' name='custom.allure.config.failed.only' toggle='true' /]

    [@ww.select cssClass="builderSelectWidget" labelKey="executable.type" name="custom.allure.config.executable" required="true"
    list=uiConfigBean.getExecutableLabels('allure') dependsOn='custom.allure.config.enabled' showOn='true'
    extraUtility=addExecutableLink /]

    [@ww.textfield labelKey="custom.allure.artifact.name.label" name="custom.allure.artifact.name" required="false"/]

    [@ww.textarea labelKey="custom.allure.logo.url.label" name="custom.allure.logo.url" required="false"/]

[/@ui.bambooSection]
