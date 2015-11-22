package de.bschandera;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class MainTest {
    private static final Path JOBS_PATH = Paths.get("./jobs");

    @Test
    public void testCheckForOpenJobPresent() throws IOException {
        Job job = new Job();
        job.setType("poll");
        job.setStatus("open");
        BufferedWriter writer = Files.newBufferedWriter(JOBS_PATH.resolve("poll.json"));
        new Gson().toJson(job, writer);
        writer.close();

        assertThat(Main.checkForOpenJob("poll").isPresent(), is(true));

        Files.delete(JOBS_PATH.resolve("poll.json"));
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

        Files.delete(JOBS_PATH.resolve("poll.json"));
    }

    @Test
    public void testCheckForOpenJobTypeBad() throws IOException {
        assertThat(Main.checkForOpenJob("poll").isPresent(), is(false));
        Job job = new Job();
        job.setType("bad type");
        job.setStatus("open");
        BufferedWriter writer = Files.newBufferedWriter(JOBS_PATH.resolve("poll.json"));
        new Gson().toJson(job, writer);
        writer.close();

        assertThat(Main.checkForOpenJob("poll").isPresent(), is(false));

        Files.delete(JOBS_PATH.resolve("poll.json"));
    }

    @Test(expected = NullPointerException.class)
    public void testCheckForOpenJobTypeNull() throws IOException {
        assertThat(Main.checkForOpenJob("poll").isPresent(), is(false));
        Job job = new Job();
        job.setStatus("open");
        BufferedWriter writer = Files.newBufferedWriter(JOBS_PATH.resolve("poll.json"));
        new Gson().toJson(job, writer);
        writer.close();

        try {
            assertThat(Main.checkForOpenJob("poll").isPresent(), is(false));
        } catch (NullPointerException e) {
            Files.delete(JOBS_PATH.resolve("poll.json"));
            throw e;
        }
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

        Files.delete(JOBS_PATH.resolve("poll.json"));
    }

    @Test(expected = NullPointerException.class)
    public void testCheckForOpenJobStateNull() throws IOException {
        assertThat(Main.checkForOpenJob("poll").isPresent(), is(false));
        Job job = new Job();
        job.setType("poll");
        BufferedWriter writer = Files.newBufferedWriter(JOBS_PATH.resolve("poll.json"));
        new Gson().toJson(job, writer);
        writer.close();

        try {
            assertThat(Main.checkForOpenJob("poll").isPresent(), is(false));
        } catch (NullPointerException e) {
            Files.delete(JOBS_PATH.resolve("poll.json"));
            throw e;
        }
    }

    @Test(expected = JsonSyntaxException.class)
    public void testCheckForOpenJobBadJson() throws IOException {
        assertThat(Main.checkForOpenJob("poll").isPresent(), is(false));
        BufferedWriter writer = Files.newBufferedWriter(JOBS_PATH.resolve("poll.json"));
        writer.append("trarala no valid json luluu");
        writer.close();

        try {
            assertThat(Main.checkForOpenJob("poll").isPresent(), is(false));
        } catch (Exception e) {
            Files.delete(JOBS_PATH.resolve("poll.json"));
            throw e;
        }
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
        Files.delete(JOBS_PATH.resolve("update-my-status"));
        assertThat(updatedJob.isPresent(), is(true));
    }
}
