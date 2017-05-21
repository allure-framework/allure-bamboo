[@ww.textfield labelKey="allure.result.directory.label" name="custom.allure.config.results.path" required="true"
  descriptionKey="allure.result.directory.description"/]

[@ww.textfield labelKey="allure.report.path.prefix.label" name="custom.allure.config.report.path" required="true"
  descriptionKey="allure.report.path.prefix.description"/]

[@ww.select cssClass="builderSelectWidget" labelKey="executable.type" name="custom.allure.config.executable" required="true"
list=uiConfigBean.getExecutableLabels('allure') extraUtility=addExecutableLink /]
