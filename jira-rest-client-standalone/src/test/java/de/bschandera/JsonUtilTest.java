package de.bschandera;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class JsonUtilTest {
    private static final Path ROOT_PATH = Paths.get(".");
    private static final Path JOBS_PATH = Paths.get("./jobs");

    @Before
    public void cleanJobsFolderAndVersionsFile() throws IOException {
        Files.deleteIfExists(ROOT_PATH.resolve("config.json"));
        if (Files.exists(JOBS_PATH)) {
            Files.list(JOBS_PATH)
                    .forEach(file -> {
                        try {
                            Files.delete(file);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        } else {
            Files.createDirectory(JOBS_PATH);
        }
    }

    @AfterClass
    public static void cleanUpAfterwards() throws IOException {
        Files.deleteIfExists(ROOT_PATH.resolve("config.json"));
        if (Files.exists(JOBS_PATH)) {
            Files.list(JOBS_PATH)
                    .forEach(file -> {
                        try {
                            Files.delete(file);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
            Files.delete(JOBS_PATH);
        }
    }

    @Test
    public void testReadConfigFile() throws IOException, URISyntaxException {
        JsonObject configJson = new JsonObject();
        configJson.addProperty("jiraUrl", "http://jira.moep.url");
        configJson.addProperty("username", "batman");
        BufferedWriter writer = Files.newBufferedWriter(ROOT_PATH.resolve("config.json"));
        new Gson().toJson(configJson, writer);
        writer.close();

        Config config = new JsonUtil().readConfigFile(ROOT_PATH.resolve("config.json"));

        assertThat(config.getJiraUrl(), equalTo(new URI("http://jira.moep.url")));
        assertThat(config.getUsername(), equalTo("batman"));

        configJson = new JsonObject();
        configJson.addProperty("username", "batman");
        writer = Files.newBufferedWriter(ROOT_PATH.resolve("config.json"));
        new Gson().toJson(configJson, writer);
        writer.close();

        config = new JsonUtil().readConfigFile(ROOT_PATH.resolve("config.json"));

        assertThat(config.getJiraUrl(), nullValue());
        assertThat(config.getUsername(), equalTo("batman"));
    }

    @Test
    public void testReadConfigFileDoesntBreakOnNotMappedFields() throws IOException, URISyntaxException {
        JsonObject configJson = new JsonObject();
        configJson.addProperty("jiraUrl", "http://jira.moep.url");
        configJson.addProperty("username", "batman");
        configJson.addProperty("some-other-config", "100.10.100.123");
        BufferedWriter writer = Files.newBufferedWriter(ROOT_PATH.resolve("config.json"));
        new Gson().toJson(configJson, writer);
        writer.close();

        Config config = new JsonUtil().readConfigFile(ROOT_PATH.resolve("config.json"));

        assertThat(config.getJiraUrl(), equalTo(new URI("http://jira.moep.url")));
        assertThat(config.getUsername(), equalTo("batman"));
    }

    @Test
    public void updateJobState() throws IOException {
        Job job = new Job();
        job.setStatus("old status");
        job.setPath("jobs/update-my-status");

        new JsonUtil().saveAndUpdateJobState(job, "new status");

        Optional<String> updatedJob = Files.lines(JOBS_PATH.resolve("update-my-status"))
                .filter(line -> line.contains("new status"))
                .findFirst();
        assertThat(updatedJob.isPresent(), is(true));
    }

    @Test
    public void testCheckForOpenJobPresent() throws IOException {
        Job job = new Job();
        job.setType("poll");
        job.setStatus("open");
        BufferedWriter writer = Files.newBufferedWriter(JOBS_PATH.resolve("poll.json"));
        new Gson().toJson(job, writer);
        writer.close();

        assertThat(new JsonUtil().checkForOpenJob("poll").isPresent(), is(true));
    }

    @Test
    public void testCheckForOpenJobNotPresent() throws IOException {
        assertThat(new JsonUtil().checkForOpenJob("poll").isPresent(), is(false));
        Job job = new Job();
        job.setType("poll");
        job.setStatus("done");
        BufferedWriter writer = Files.newBufferedWriter(JOBS_PATH.resolve("poll.json"));
        new Gson().toJson(job, writer);
        writer.close();

        assertThat(new JsonUtil().checkForOpenJob("poll").isPresent(), is(false));
    }

    @Test
    public void testCheckForOpenJobTypeBad() throws IOException {
        Job job = new Job();
        job.setType("bad type");
        job.setStatus("open");
        BufferedWriter writer = Files.newBufferedWriter(JOBS_PATH.resolve("poll.json"));
        new Gson().toJson(job, writer);
        writer.close();

        assertThat(new JsonUtil().checkForOpenJob("poll").isPresent(), is(false));
    }

    @Test(expected = NullPointerException.class)
    public void testCheckForOpenJobTypeNull() throws IOException {
        Job job = new Job();
        job.setStatus("open");
        BufferedWriter writer = Files.newBufferedWriter(JOBS_PATH.resolve("poll.json"));
        new Gson().toJson(job, writer);
        writer.close();

        assertThat(new JsonUtil().checkForOpenJob("poll").isPresent(), is(false));
    }

    @Test
    public void testCheckForOpenJobBadState() throws IOException {
        assertThat(new JsonUtil().checkForOpenJob("poll").isPresent(), is(false));
        Job job = new Job();
        job.setType("poll");
        job.setStatus("bad state");
        BufferedWriter writer = Files.newBufferedWriter(JOBS_PATH.resolve("poll.json"));
        new Gson().toJson(job, writer);
        writer.close();

        assertThat(new JsonUtil().checkForOpenJob("poll").isPresent(), is(false));
    }

    @Test(expected = NullPointerException.class)
    public void testCheckForOpenJobStateNull() throws IOException {
        assertThat(new JsonUtil().checkForOpenJob("poll").isPresent(), is(false));
        Job job = new Job();
        job.setType("poll");
        BufferedWriter writer = Files.newBufferedWriter(JOBS_PATH.resolve("poll.json"));
        new Gson().toJson(job, writer);
        writer.close();

        assertThat(new JsonUtil().checkForOpenJob("poll").isPresent(), is(false));
    }

    @Test(expected = JsonSyntaxException.class)
    public void testCheckForOpenJobBadJson() throws IOException {
        assertThat(new JsonUtil().checkForOpenJob("poll").isPresent(), is(false));
        BufferedWriter writer = Files.newBufferedWriter(JOBS_PATH.resolve("poll.json"));
        writer.append("trarala no valid json luluu");
        writer.close();

        assertThat(new JsonUtil().checkForOpenJob("poll").isPresent(), is(false));
    }

}
