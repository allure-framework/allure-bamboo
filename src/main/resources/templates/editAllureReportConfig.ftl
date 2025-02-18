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
    [@ww.checkbox labelKey='custom.allure.config.download.enabled.label' name='downloadEnabled' toggle='true' /]

    [@ww.checkbox labelKey='custom.allure.config.logo.enabled.label' name='customLogoEnabled' toggle='false' /]

    [@ww.checkbox labelKey='custom.allure.config.enabled.default.label' name='enabledByDefault' toggle='true' /]

    [@ww.checkbox labelKey='custom.allure.config.reports.cleanup.enabled.label' name='enabledReportsCleanup' toggle='false' /]

    [@ww.textfield labelKey='custom.allure.config.download.url.label' name='localStoragePath' required='true'/]

    [@ww.textfield labelKey='custom.allure.config.local.storage.label' name='downloadBaseUrl' required='true'/]
[/@ww.form]
</body>
</html>