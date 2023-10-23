[#-- @ftlvariable name="action" type="io.qameta.allure.bamboo.ConfigureAllureReportAction" --]
[#-- @ftlvariable name="" type="io.qameta.allure.bamboo.ConfigureAllureReportAction" --]

<html>
<head>
    <title>[@ww.text name='allure.plugin.title' /]</title>
    <meta name="decorator" content="adminpage">
    <meta name="adminCrumb" content="editAllureReportConfig">
</head>
<body>
<h1>[@ww.text name='allure.plugin.title' /]</h1>

<p>[@ww.text name='admin.allureReportConfig.description' /]</p>

[@ww.form action="saveAllureReportConfig"
id="saveAllureReportConfigForm"
submitLabelKey='global.buttons.update'
titleKey='admin.allureReportConfig.edit.title'
cancelUri='/admin/editAllureReportConfig.action'
]
    [@ww.checkbox labelKey='allure.config.download.enabled.label' name='custom.allure.config.download.enabled' toggle='true' /]

    [@ww.checkbox labelKey='allure.config.logo.enabled.label' name='custom.allure.config.logo.enabled' toggle='true' /]

    [@ww.checkbox labelKey='allure.config.enabled.default.label' name='custom.allure.config.enabled.default' toggle='true' /]

    [@ww.textfield labelKey="allure.config.download.url.label" name="custom.allure.config.download.url" required="true"/]

    [@ww.textfield labelKey="allure.config.local.storage.label" name="custom.allure.config.local.storage" required="true"/]
[/@ww.form]
</body>
</html>
