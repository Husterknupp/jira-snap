<#-- @ftlvariable name="" type="de.bschandera.jiraversioncleaner.resources.CleanerView" -->
<html xmlns="http://www.w3.org/1999/html">
<head>
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

<form method="POST" action="/versions">
        <input type="hidden" name="refreshVersions" value="true"/>
        <input type="submit" value="Update version list plx"/>
</form>

<#if !(message == "")>
    <div>
        ${message}<br/><br/>
    </div>
</#if>

<div>
<#list versions?keys as component>
    <b>${component}</b>
    <#list versions[component] as version>
        ${version.name}<br/>
        <form method="post" action="/versions">
            <fieldset>
                Release Date<br>
                <input type="text" name="releaseDate" value="e.g., 31.10.2015 10:15:31" size="30"/>
                <input type="hidden" name="versionName" value="${version.name}"/>
                <input type="hidden" name="refreshVersions" value="true"/>
                <br><br>
                <input type="submit" value="Release za version"/>
            </fieldset>
        </form>
    </#list>
</#list>
</div>

</body>
</html>
