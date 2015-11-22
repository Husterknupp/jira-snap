package de.bschandera;

import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.atlassian.jira.rest.client.ProjectRestClient;
import com.atlassian.jira.rest.client.domain.Project;
import com.atlassian.jira.rest.client.domain.input.VersionInput;
import com.atlassian.jira.rest.client.internal.jersey.JerseyJiraRestClientFactory;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.sun.management.UnixOperatingSystemMXBean;
import org.joda.time.DateTime;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Type;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class Main {
    private static final Gson GSON = new Gson();
    private static final Path ROOT_PATH = Paths.get(".");
    private static final Path VERSIONS_PATH = Paths.get("versions.json");

    private static JiraRestClient restClient;

    public static void main(String[] args) throws InterruptedException, IOException {
        restClient = configureRestClient();
        for (; true; Thread.sleep(100)) {
            checkForOpenJob("poll").map(job -> {
                printNoOfOpenFiles();
                System.out.println("[INFO] Get versions for " + job.getComponentsWanted());
                try {
                    List<Version> result = getUnreleasedVersions(job.getComponentsWanted());
                    saveAndUpdateJobState(job, "done");
                    return result;
                } catch (Exception e) {
                    System.out.println("[ERROR] " + e.getMessage());
                    saveAndUpdateJobState(job, "failed");
                    return Collections.<Version>emptyList();
                }
            }).ifPresent(writeToFile());

            checkForOpenJob("update").ifPresent(job -> {
                try {
                    job.getUpdatedVersions().stream().forEach(Main::releaseVersion);
                    saveAndUpdateJobState(job, "done");
                } catch (Exception e) {
                    System.out.println("[ERROR] " + e.getMessage());
                    saveAndUpdateJobState(job, "failed");
                }
            });
        }
    }

    private static JiraRestClient configureRestClient() {
        JiraRestClient restClient;
        try {
            Config config = readConfigFile("config.json");
            restClient = new JerseyJiraRestClientFactory()
                    .createWithBasicHttpAuthentication(config.getJiraUrl(), config.getUsername(), config.getPassword());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return restClient;
    }

    private static Config readConfigFile(String filePath) throws IOException {
        Config config;
        try (BufferedReader reader = Files.newBufferedReader(ROOT_PATH.resolve(filePath))) {
            config = GSON.fromJson(reader, Config.class);
        } catch (JsonSyntaxException e) {
            System.out.println("[ERROR] json syntax problem in file " + filePath + " " + e.getMessage());
            throw new RuntimeException(e);
        }
        return config;
    }

    static Optional<Job> checkForOpenJob(String jobType) throws IOException {
        DirectoryStream<Path> jobFilesStream = Files.newDirectoryStream(ROOT_PATH.resolve("jobs"));
        Optional<Job> result = Optional.empty();
        for (Path aJobFilesStream : jobFilesStream) {
            Job job;
            try (BufferedReader reader = Files.newBufferedReader(aJobFilesStream)) {
                job = GSON.fromJson(reader, Job.class);
                if (job == null) {
                    continue;
                }
            } catch (JsonSyntaxException e) {
                System.out.println("[ERROR] json syntax problem in file " + aJobFilesStream.toString() + " " + e.getMessage());
                throw e;
            }
            if (job.getStatus().equalsIgnoreCase("open") && job.getType().equalsIgnoreCase(jobType)) {
                job.setPath(aJobFilesStream.toString());
                System.out.println("[INFO] found open job: " + jobType + " " + job.getPath());
                result = Optional.of(job);
                break;
            }
        }
        jobFilesStream.close();
        return result;
    }

    private static List<Version> getUnreleasedVersions(List<String> components) {
        ProjectRestClient projects = restClient.getProjectClient();
        Project dev = projects.getProject("DEV", new NullProgressMonitor());
        List<Version> ourUnreleasedVersions = new ArrayList<>();
        Lists.newArrayList(dev.getVersions()).stream()
                .map(Version::ofJiraVersion)
                .forEach(version -> {
                    if (isOneOfOurVersions(components, version) && !version.isReleased()) {
                        ourUnreleasedVersions.add(version);
                    }
                });
        return ourUnreleasedVersions;
    }

    private static boolean isOneOfOurVersions(List<String> components, Version version) {
        return Iterables.tryFind(components, component -> version.getName().startsWith(component)).isPresent();
    }

    static void saveAndUpdateJobState(Job job, String newStatus) {
        try {
            job.setStatus(newStatus);
            BufferedWriter writer = Files.newBufferedWriter(Paths.get(job.getPath()));
            GSON.toJson(job, writer);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Consumer<? super List<Version>> writeToFile() {
        Type srcTypeVersionList = new TypeToken<List<Version>>() {
        }.getType();
        return versions -> {
            if (versions.size() == 0) {
                System.out.println("[INFO] Found 0 unreleased versions.");
                return;
            }

            try {
                BufferedWriter writer = Files.newBufferedWriter(VERSIONS_PATH);
                GSON.toJson(versions, srcTypeVersionList, writer);
                writer.close();
                System.out.println("[INFO] Found " + versions.size() + " unreleased versions. Put them in " + VERSIONS_PATH);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static void releaseVersion(Version version) {
        boolean released = true;
        VersionInput versi0n = VersionInput.create("DEV", null, null, new DateTime(version.getReleaseDate()), version.isArchived(), released);
        restClient.getVersionRestClient().updateVersion(version.getSelf(), versi0n, new NullProgressMonitor());
        System.out.println("[INFO] Released version " + version.getSelf());
    }

    private static void printNoOfOpenFiles() {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        if (os instanceof UnixOperatingSystemMXBean) {
            System.out.println("[DEBUG] Number of open file descriptors: " + ((UnixOperatingSystemMXBean) os).getOpenFileDescriptorCount());
        }
    }
}
