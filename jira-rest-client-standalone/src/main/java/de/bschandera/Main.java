package de.bschandera;

import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.atlassian.jira.rest.client.ProjectRestClient;
import com.atlassian.jira.rest.client.domain.Project;
import com.atlassian.jira.rest.client.domain.Version;
import com.atlassian.jira.rest.client.domain.input.VersionInput;
import com.atlassian.jira.rest.client.internal.jersey.JerseyJiraRestClientFactory;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class Main {
    private static final URI JIRA_URI;
    private static final JiraRestClient REST_CLIENT;
    private static final Gson GSON = new Gson();

    static {
        try {
            JIRA_URI = new URI("https://jira.spreadomat.net/");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        REST_CLIENT = new JerseyJiraRestClientFactory().createWithBasicHttpAuthentication(JIRA_URI, "user", "pw");
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        String versionsFilePath = "versions.json";
        String jobsPath = "./jobs";

        for (; true; Thread.sleep(100)) {
            checkForOpenJob(jobsPath, "poll").map(job -> {
                System.out.println("[INFO] Start polling for " + job.getComponentsWanted());
                List<Version> result = getUnreleasedVersions(job.getComponentsWanted());
                updateJobState(job, "done");
                System.out.println("[INFO] Found " + result.size() + " unreleased versions. Find them in " + versionsFilePath);
                return result;
            }).ifPresent(writeToFile(versionsFilePath));

            checkForOpenJob(jobsPath, "update").ifPresent(job ->
                    job.getUpdatedVersions().stream().forEach(Main::releaseVersion));
        }
    }

    private static Optional<Job> checkForOpenJob(String jobsPath, String jobType) throws IOException {
        return Files.list(Paths.get(jobsPath))
                .map(jobFile -> {
                    try {
                        Job job = GSON.fromJson(Files.newBufferedReader(jobFile), Job.class);
                        job.setPath(jobFile.toString());
                        return job;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .filter(job -> job.getStatus().equalsIgnoreCase("open"))
                .filter(job -> job.getType().equalsIgnoreCase(jobType))
                .findFirst();
    }

    private static List<Version> getUnreleasedVersions(List<String> components) {
        ProjectRestClient projects = REST_CLIENT.getProjectClient();
        Project dev = projects.getProject("DEV", new NullProgressMonitor());
        List<Version> ourUnreleasedVersions = new ArrayList<>();
        dev.getVersions().forEach(version -> {
            if (isOneOfOurVersions(components, version) && !version.isReleased()) {
                ourUnreleasedVersions.add(version);
            }
        });
        return ourUnreleasedVersions;
    }

    private static boolean isOneOfOurVersions(List<String> components, Version version) {
        return Iterables.tryFind(components, component -> version.getName().startsWith(component)).isPresent();
    }

    private static void updateJobState(Job job, String newStatus) {
        try {
            job.setStatus(newStatus);
            BufferedWriter writer = Files.newBufferedWriter(Paths.get(job.getPath()));
            GSON.toJson(job, writer);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Consumer<? super List<Version>> writeToFile(String versionsFilePath) {
        Type srcTypeVersionList = new TypeToken<List<Version>>() {
        }.getType();
        return versions -> {
            try {
                BufferedWriter writer = Files.newBufferedWriter(Paths.get(versionsFilePath));
                GSON.toJson(versions, srcTypeVersionList, writer);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
    }

    private static void releaseVersion(Version version) {
        boolean released = true;
        VersionInput versi0n = VersionInput.create("DEV", null, null, version.getReleaseDate(), version.isArchived(), released);
        REST_CLIENT.getVersionRestClient().updateVersion(version.getSelf(), versi0n, new NullProgressMonitor());
        System.out.println("[INFO] " + version.getSelf() + " released");
    }

}
