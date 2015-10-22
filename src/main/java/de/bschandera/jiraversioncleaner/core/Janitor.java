package de.bschandera.jiraversioncleaner.core;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.ProjectRestClient;
import com.atlassian.jira.rest.client.api.VersionRestClient;
import com.atlassian.jira.rest.client.api.domain.Project;
import com.atlassian.jira.rest.client.api.domain.Version;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Janitor {

    public Map<String, List<String>> getVersions() {
        final URI jiraUri;
        try {
            jiraUri = new URI("https://jira.spreadomat.net/");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        final JiraRestClient restClient = new AsynchronousJiraRestClientFactory()
                .createWithBasicHttpAuthentication(jiraUri, "USERNAME", "PASSWORD");

        // todo crawl part
        List<String> components = Arrays.asList("component A", "component b");
        ProjectRestClient projects = restClient.getProjectClient();
        Project dev = projects.getProject("DEV").claim();
        List<Version> ourUnreleasedVersions = Lists.newArrayList(dev.getVersions()).stream()
                .filter(version -> isOneOfOurVersions(components, version))
                .filter(version -> !version.isReleased()).collect(Collectors.toList());
        ourUnreleasedVersions.forEach(System.out::println);
        Map<String, List<String>> result = new HashMap<>();
        result.put("all", ourUnreleasedVersions.stream().map(Version::getName).collect(Collectors.toList()));
        if (true) return result;

        // todo update part
        VersionRestClient versions = restClient.getVersionRestClient();
        DateTime now = new DateTime(new Date().getTime());
        Version version = Lists.newArrayList(dev.getVersions()).stream()
                .filter(v -> v.getName().equals("component-version-1.0.0")).findFirst().get();
//        Version version = ourUnreleasedVersions.get(0);
        DateTime releaseDate = new DateTime().withDate(2015, 8, 13);
//        versions.updateVersion(version.getSelf(),
//                VersionInput.create(null, null, null, releaseDate, version.isArchived(), true), new NullProgressMonitor());
        System.out.println("done");
        return Collections.emptyMap();
    }

    private boolean isOneOfOurVersions(List<String> components, Version version) {
        return Iterables.tryFind(components, component -> version.getName().startsWith(component)).isPresent();
    }
}
