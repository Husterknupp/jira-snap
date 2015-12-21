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
import com.google.gson.reflect.TypeToken;
import com.sun.management.UnixOperatingSystemMXBean;
import org.joda.time.DateTime;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Type;
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
    private static Password password = new Password();
    private static JsonUtil jsonUtil = new JsonUtil();

    public static void main(String[] args) throws InterruptedException, IOException {
        if (restClient == null) {
            Optional<String> configFileName = getConfigFileOption(args);
            restClient = password.readFromConsole()
                    .map(pw -> configureRestClient(pw, configFileName)).orElse(null);
            if (restClient == null) {
                System.out.println("[WARN] No jira rest client configured. The end.");
                return;
            }
        }
        if (args.length != 0 && !args[0].startsWith("-h") && !args[0].startsWith("-c")) {
            System.out.println("[INFO] Test mode. I will execute only one of each job if present.");
            System.out.println("[INFO] Provide no arguments to run in normal mode.");
            pollIfOpen();
            updateIfOpen();
            return;
        }
        for (; true; Thread.sleep(100)) {
            pollIfOpen();
            updateIfOpen();
        }
    }

    private static Optional<String> getConfigFileOption(String[] args) {
        if (args.length >= 2 && args[0].startsWith("-c")) {
            return Optional.of(args[1]);
        } else if (args.length >= 1 && args[0].startsWith("-h")) {
            System.out.println("[INFO]\toption -c\tusage:\t-c my-config.json");
            System.exit(0);
            return Optional.empty();
        } else {
            return Optional.empty();
        }
    }

    private static JiraRestClient configureRestClient(String password, Optional<String> configFileName) {
        Config config = jsonUtil.readConfigFile(ROOT_PATH.resolve(configFileName
                .orElseGet(() -> {
                    System.out.println("[INFO] Read configuration from config.json. Provide -c option to specify file");
                    return "config.json";
                })));
        return new JerseyJiraRestClientFactory().createWithBasicHttpAuthentication(
                config.getJiraUrl(), config.getUsername(), password);
    }

    private static void updateIfOpen() throws IOException {
        jsonUtil.checkForOpenJob("update").ifPresent(job -> {
            try {
                job.getUpdatedVersions().stream().forEach(Main::releaseVersion);
                jsonUtil.saveAndUpdateJobState(job, "done");
            } catch (Exception e) {
                System.out.println("[ERROR] " + e.getMessage());
                jsonUtil.saveAndUpdateJobState(job, "failed");
            }
        });
    }

    private static void pollIfOpen() throws IOException {
        jsonUtil.checkForOpenJob("poll").map(job -> {
            printNoOfOpenFiles();
            System.out.println("[INFO] Get versions for " + job.getComponentsWanted());
            try {
                List<Version> result = getUnreleasedVersions(job.getComponentsWanted());
                jsonUtil.saveAndUpdateJobState(job, "done");
                return result;
            } catch (Exception e) {
                System.out.println("[ERROR] " + e.getMessage());
                jsonUtil.saveAndUpdateJobState(job, "failed");
                return Collections.<Version>emptyList();
            }
        }).ifPresent(writeToFile());
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

    static void setJiraClient(JiraRestClient client) {
        restClient = client;
    }

    static void setPassword(Password newPassword) {
        password = newPassword;
    }
}
