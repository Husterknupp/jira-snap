<#-- @ftlvariable name="" type="de.bschandera.jiraversioncleaner.resources.CleanerView" -->
<html>
<body>
<!-- calls CleanerView.getConfiguredComponents() and sanitizes it -->
<h1>Jira Versions</h1>
<#list configuredComponents as component>
<b>${component}</b>
</#list>
<#list versions?keys as component>
<b>${component}</b>
versions[component]
</#list>
Versions
</body>
</html>
