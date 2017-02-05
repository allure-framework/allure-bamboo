[#assign addExecutableLink][@ui.displayAddExecutableInline executableKey="allure"/][/#assign]
[@ww.select cssClass="builderSelectWidget" labelKey="executable.type" name="executable.label" required="true"
            list=uiConfigBean.getExecutableLabels('allure')
            extraUtility=addExecutableLink /]

[@ww.label labelKey="allure.result.directory.label" name="allure.result.directory"/]
[@ww.label labelKey="allure.report.path.prefix.label" name="allure.report.path.prefix"/]