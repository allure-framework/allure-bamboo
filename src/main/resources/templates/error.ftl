[#-- @ftlvariable name="action" type="com.atlassian.bamboo.logger.AdminErrorAction" --]
[#-- @ftlvariable name="" type="com.atlassian.bamboo.logger.AdminErrorAction" --]

<html>
<head>
	<title>[@ww.text name='error.title' /]</title>
    <meta name="decorator" content="errorDecorator"/>
</head>

<body>
    [@ui.header pageKey='error.heading' /]

    [#if formattedActionErrors?has_content]
        [@ui.messageBox type='warning']
            [#list formattedActionErrors as error]
                <p>${error}</p>
            [/#list]
        [/@ui.messageBox]
    [/#if]
</body>
</html>