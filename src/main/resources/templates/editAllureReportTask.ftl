[@ww.textfield labelKey="allure.result.directory.label" name="allureResultDirectory" required="true"
  descriptionKey="allure.result.directory.description"/]

[@ww.textfield labelKey="allure.report.path.prefix.label" name="allureReportPathPrefix" required="true"
  descriptionKey="allure.report.path.prefix.description"/]

[@ww.select cssClass="builderSelectWidget" labelKey="executable.type" name="custom.allure.config.executable" required="true"
list=uiConfigBean.getExecutableLabels('allure') extraUtility=addExecutableLink /]
