[#assign addExecutableLink][@ui.displayAddExecutableInline executableKey="allure"/][/#assign]
[@ww.select cssClass="builderSelectWidget" labelKey="executable.type" name="executableLabel" required="true"
            list=uiConfigBean.getExecutableLabels('allure')
            extraUtility=addExecutableLink /]

[@ww.label labelKey="allure.result.directory.label" name="allureResultDirectory"/]
[@ww.label labelKey="allure.report.path.prefix.label" name="allureReportPathPrefix"/]