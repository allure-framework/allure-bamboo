[#assign addExecutableLink][@ui.displayAddExecutableInline executableKey="allure"/][/#assign]
[@ww.select cssClass="builderSelectWidget" labelKey="executable.type" name="executable.label" required="true"
            list=uiConfigBean.getExecutableLabels('allure')
            extraUtility=addExecutableLink /]

[@ww.textfield labelKey="allure.result.directory.label" name="allure.result.directory" required="true"
  descriptionKey="allure.result.directory.description"/]
[@ww.textfield labelKey="allure.report.path.prefix.label" name="allure.report.path.prefix" required="true"
  descriptionKey="allure.report.path.prefix.description"/]