package de.bschandera;

import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.ProjectRestClient;
import com.atlassian.jira.rest.client.VersionRestClient;
import com.atlassian.jira.rest.client.domain.Project;
import com.atlassian.jira.rest.client.domain.Version;
import com.atlassian.jira.rest.client.domain.input.VersionInput;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.stringContainsInOrder;

public class MainTest {
    private static final Path JOBS_PATH = Paths.get("./jobs");
    private static final Path ROOT_PATH = Paths.get(".");

    @Before
    public void cleanJobsFolderAndVersionsFile() throws IOException {
        Files.deleteIfExists(ROOT_PATH.resolve("versions.json"));
        Files.deleteIfExists(ROOT_PATH.resolve("config.json"));
        Files.list(JOBS_PATH)
                .forEach(file -> {
                    try {
                        Files.delete(file);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }

    @AfterClass
    public static void cleanUpAfterwards() throws IOException {
        Files.deleteIfExists(ROOT_PATH.resolve("versions.json"));
        Files.deleteIfExists(ROOT_PATH.resolve("config.json"));
        Files.list(JOBS_PATH)
                .forEach(file -> {
                    try {
                        Files.delete(file);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }

    @Test
    public void testCheckForOpenJobPresent() throws IOException {
        Job job = new Job();
        job.setType("poll");
        job.setStatus("open");
        BufferedWriter writer = Files.newBufferedWriter(JOBS_PATH.resolve("poll.json"));
        new Gson().toJson(job, writer);
        writer.close();

        assertThat(Main.checkForOpenJob("poll").isPresent(), is(true));
    }

    @Test
    public void testCheckForOpenJobNotPresent() throws IOException {
        assertThat(Main.checkForOpenJob("poll").isPresent(), is(false));
        Job job = new Job();
        job.setType("poll");
        job.setStatus("done");
        BufferedWriter writer = Files.newBufferedWriter(JOBS_PATH.resolve("poll.json"));
        new Gson().toJson(job, writer);
        writer.close();

        assertThat(Main.checkForOpenJob("poll").isPresent(), is(false));
    }

    @Test
    public void testCheckForOpenJobTypeBad() throws IOException {
        Job job = new Job();
        job.setType("bad type");
        job.setStatus("open");
        BufferedWriter writer = Files.newBufferedWriter(JOBS_PATH.resolve("poll.json"));
        new Gson().toJson(job, writer);
        writer.close();

        assertThat(Main.checkForOpenJob("poll").isPresent(), is(false));
    }

    @Test(expected = NullPointerException.class)
    public void testCheckForOpenJobTypeNull() throws IOException {
        Job job = new Job();
        job.setStatus("open");
        BufferedWriter writer = Files.newBufferedWriter(JOBS_PATH.resolve("poll.json"));
        new Gson().toJson(job, writer);
        writer.close();

        assertThat(Main.checkForOpenJob("poll").isPresent(), is(false));
    }

    @Test
    public void testCheckForOpenJobBadState() throws IOException {
        assertThat(Main.checkForOpenJob("poll").isPresent(), is(false));
        Job job = new Job();
        job.setType("poll");
        job.setStatus("bad state");
        BufferedWriter writer = Files.newBufferedWriter(JOBS_PATH.resolve("poll.json"));
        new Gson().toJson(job, writer);
        writer.close();

        assertThat(Main.checkForOpenJob("poll").isPresent(), is(false));
    }

    @Test(expected = NullPointerException.class)
    public void testCheckForOpenJobStateNull() throws IOException {
        assertThat(Main.checkForOpenJob("poll").isPresent(), is(false));
        Job job = new Job();
        job.setType("poll");
        BufferedWriter writer = Files.newBufferedWriter(JOBS_PATH.resolve("poll.json"));
        new Gson().toJson(job, writer);
        writer.close();

        assertThat(Main.checkForOpenJob("poll").isPresent(), is(false));
    }

    @Test(expected = JsonSyntaxException.class)
    public void testCheckForOpenJobBadJson() throws IOException {
        assertThat(Main.checkForOpenJob("poll").isPresent(), is(false));
        BufferedWriter writer = Files.newBufferedWriter(JOBS_PATH.resolve("poll.json"));
        writer.append("trarala no valid json luluu");
        writer.close();

        assertThat(Main.checkForOpenJob("poll").isPresent(), is(false));
    }

    @Test
    public void updateJobState() throws IOException {
        Job job = new Job();
        job.setStatus("old status");
        job.setPath("jobs/update-my-status");

        Main.saveAndUpdateJobState(job, "new status");

        Optional<String> updatedJob = Files.lines(JOBS_PATH.resolve("update-my-status"))
                .filter(line -> line.contains("new status"))
                .findFirst();
        assertThat(updatedJob.isPresent(), is(true));
    }

    @Test
    public void testPasswordIsUsed() throws IOException, InterruptedException {
        Password password = Mockito.mock(Password.class);
        Mockito.when(password.readFromConsole()).thenReturn(Optional.of("pw"));
        Main.setPassword(password);

        try {
            Main.main(new String[]{"test mode"});
        } catch (UncheckedIOException e) {
            // expected due to missing config.json - continue with verify
        }
        Mockito.verify(password, Mockito.times(1)).readFromConsole();
    }

    @Test(expected = UncheckedIOException.class)
    public void mainIntegrationTestNoConfigFile() throws IOException, InterruptedException {
        assertThat("Please remove config.json", Files.exists(ROOT_PATH.resolve("config.json")), is(false));
        Main.main(new String[]{});
    }

    @Test
    public void mainIntegrationTestNoJobFile() throws IOException, InterruptedException {
        BufferedWriter writer = Files.newBufferedWriter(ROOT_PATH.resolve("config.json"));
        writer.append("{\n" +
                "  \"jiraUrl\": \"http://funny.url\",\n" +
                "  \"username\": \"user\",\n" +
                "  \"password\": \"password\"\n" +
                "}");
        writer.close();
        assertThat(Files.list(JOBS_PATH).count(), is(0l));

        Main.main(new String[]{"anyString"});

        assertThat(Files.list(JOBS_PATH).count(), is(0l));
        Files.delete(ROOT_PATH.resolve("config.json"));
    }

    @Test
    public void pollingIntegrationTest() throws URISyntaxException, IOException, InterruptedException {
        // place an open poll job
        assertThat(Files.exists(ROOT_PATH.resolve("versions.json")), is(false));
        assertThat(Files.list(JOBS_PATH).count(), is(0l));
        Job job = new Job();
        job.setType("poll");
        job.setStatus("open");
        job.setComponentsWanted(Collections.singletonList("component-name"));
        BufferedWriter writer = Files.newBufferedWriter(JOBS_PATH.resolve("poll.json"));
        new Gson().toJson(job, writer);
        writer.close();

        // mock jira rest client to return one of our versions
        ProjectRestClient projectClient = Mockito.mock(ProjectRestClient.class);
        Project dev = Mockito.mock(Project.class);
        Long id = null;
        boolean archived = false;
        boolean released = false;
        DateTime releaseDate = null;
        Mockito.when(dev.getVersions()).thenReturn(Collections.singletonList(
                new Version(new URI("http://jira.company.net"), id, "component-name-1.0.0", "description", archived, released, releaseDate)));
        Mockito.when(projectClient.getProject(Mockito.eq("DEV"), Mockito.any())).thenReturn(dev);
        JiraRestClient jiraClient = Mockito.mock(JiraRestClient.class);
        Mockito.when(jiraClient.getProjectClient()).thenReturn(projectClient);
        Main.setJiraClient(jiraClient);

        Main.main(new String[]{"one run"});

        assertThat(Files.list(JOBS_PATH).count(), is(1l));
        String jobFile = Files.lines(JOBS_PATH.resolve("poll.json"))
                .reduce((left, right) -> String.join("\n", left, right))
                .orElseGet(() -> "");
        assertThat(jobFile, containsString("\"status\":\"done\""));

        assertThat(Files.exists(ROOT_PATH.resolve("versions.json")), is(true));
        String file = Files.lines(ROOT_PATH.resolve("versions.json"))
                .reduce((left, right) -> String.join("\n", left, right))
                .orElseGet(() -> "");
        assertThat(file, stringContainsInOrder(Arrays.asList("jira.company.net", "component-name-1.0.0", "\"isReleased\":false")));
        assertThat(file, not(containsString("description")));
    }

    @Test
    public void updateIntegrationTest() throws IOException, InterruptedException {
        // place an open update job
        assertThat(Files.exists(ROOT_PATH.resolve("versions.json")), is(false));
        assertThat(Files.list(JOBS_PATH).count(), is(0l));
        Job job = new Job();
        job.setType("update");
        job.setStatus("open");
        de.bschandera.Version version = new de.bschandera.Version();
        version.setIsReleased(true);
        version.setName("version-name-1.0.0");
        Date releaseDate = new Date();
        version.setReleaseDate(releaseDate);
        version.setSelf(URI.create("http://jira.net/my-cool-version"));
        job.setUpdatedVersions(Collections.singletonList(version));
        BufferedWriter writer = Files.newBufferedWriter(JOBS_PATH.resolve("update.json"));
        new Gson().toJson(job, writer);
        writer.close();

        VersionRestClient versionClient = Mockito.mock(VersionRestClient.class);
        JiraRestClient jiraClient = Mockito.mock(JiraRestClient.class);
        Mockito.when(jiraClient.getVersionRestClient()).thenReturn(versionClient);
        Main.setJiraClient(jiraClient);

        Main.main(new String[]{"one run"});
        ArgumentCaptor<VersionInput> argumentCaptor = ArgumentCaptor.forClass(VersionInput.class);
        Mockito.verify(versionClient)
                .updateVersion(Matchers.eq(URI.create("http://jira.net/my-cool-version")), argumentCaptor.capture(), Mockito.any());

        assertThat(Files.list(JOBS_PATH).count(), is(1l));
        String jobFile = Files.lines(JOBS_PATH.resolve("update.json"))
                .reduce((left, right) -> String.join("\n", left, right))
                .orElseGet(() -> "");
        assertThat(jobFile, containsString("\"status\":\"done\""));

        VersionInput updatedVersion = argumentCaptor.getValue();
        assertThat(updatedVersion.getProjectKey(), equalTo("DEV"));
        assertThat(updatedVersion.getReleaseDate().toDate().toString(), equalTo(releaseDate.toString())); // because that's how its serialized
        assertThat(updatedVersion.isReleased(), is(true));
        assertThat(updatedVersion.isArchived(), is(false));
        assertThat(updatedVersion.getDescription(), nullValue());
        assertThat(updatedVersion.getName(), nullValue());
    }
}
