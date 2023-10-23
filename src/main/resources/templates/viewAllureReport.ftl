[#-- @ftlvariable name="" type="io.qameta.allure.bamboo.ViewAllureReport" --]
<html>
<head>
    [#assign reportUrl = "${baseUrl}/plugins/servlet/allure/report/${planKey}/${buildNumber}/"]
    [#assign reportZipUrl = "${baseUrl}/plugins/servlet/allure/report/${planKey}/${buildNumber}/report.zip"]
    [@ui.header pageKey='buildResult.changes.title' object='${immutablePlan.name} ${resultsSummary.buildNumber}' title=true/]
    <meta name="tab" content="allure"/>
    <script type="text/javascript">
        (function () {
            (window.AJS || (window.AJS = {}));

            AJS.$(function ($) {
                $("#allure-report-expand-link").click(function () {
                    try {
                        window.open(document.getElementById('allure-report-frame').contentWindow.location.href, '_blank');
                    } catch (e) {
                        window.open('${reportUrl}', '_blank');
                    }
                });
            });
        }());
    </script>
</head>
<body>
<a style="float: left;" id="allure-report-expand-link" target="_blank" href="${reportUrl}">Expand</a>&nbsp;
<a style="float: right;" id="allure-report-export-link" target="_blank" href="${reportZipUrl}">Download</a>&nbsp;
<iframe id="allure-report-frame" src="${reportUrl}" style="border: 0; width: 100%; height: 840px;"></iframe>
</body>
</html>
