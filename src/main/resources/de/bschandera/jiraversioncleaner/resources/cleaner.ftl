<#-- @ftlvariable name="" type="de.bschandera.jiraversioncleaner.resources.CleanerView" -->
<html xmlns="http://www.w3.org/1999/html">
<head>
    <script>
        function requestSmart(method, url) {
            var xmlHttp = new XMLHttpRequest();
            xmlHttp.onreadystatechange = function () {
                if (xmlHttp.readyState == 4 && xmlHttp.status == 200)
                    callback(xmlHttp.responseText);
            };
            xmlHttp.open(method, url, true); // true for asynchronous
            xmlHttp.send(null);
            location.reload();
        }
    </script>
</head>
<body>
<!-- calls CleanerView.getConfiguredComponents() and sanitizes it -->
<h1>Jira Versions</h1>

<div>
    <b>Configured Components</b>

    <div>
    <#list configuredComponents as component>
        ${component}
    </#list>
    </div>
</div>
<br>

<div>
<#list versions?keys as component>
    <b>${component}</b>

    <#list versions[component] as version>
        <div>
        ${version.name}
            <button type="button"
                    class="btn btn-sm  btn-danger"
                    style="width: 200px"
                    onclick="requestSmart('PUT', 'http://localhost:2000/versions/${version.name}?isReleased=true')"
                    href="javascript:void(0);">Release version
            </button>
        </div>
    </#list>
</#list>
</div>
</body>
</html>
