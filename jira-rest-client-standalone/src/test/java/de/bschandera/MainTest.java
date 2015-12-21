package de.bschandera;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class MainTest {
    private static final Path JOBS_PATH = Paths.get("./jobs");
    private static final Path ROOT_PATH = Paths.get(".");

    @Before
    public void cleanJobsFolderAndVersionsFile() throws IOException {
        Files.deleteIfExists(ROOT_PATH.resolve("versions.json"));
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
        Files.createDirectory(JOBS_PATH);
        Main.setJiraClient(null);
        Main.setPassword(new Password());
    }

    @AfterClass
    public static void cleanUpAfterwards() throws IOException {
        Files.deleteIfExists(ROOT_PATH.resolve("versions.json"));
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

}
