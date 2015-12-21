package de.bschandera;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class JsonUtil {
    private static final Gson GSON = new Gson();

    public Config readConfigFile(Path filePath) {
        Config config;
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            config = GSON.fromJson(reader, Config.class);
        } catch (JsonSyntaxException e) {
            System.out.println("[ERROR] json syntax problem in file " + filePath + " " + e.getMessage());
            throw e;
        } catch (IOException e) {
            System.out.println("[ERROR] I/O problem in file " + filePath);
            throw new UncheckedIOException(e);
        }
        return config;
    }

    public Optional<Job> checkForOpenJob(String jobType) {
        return null;
    }

    public void saveAndUpdateJobState(Job job, String newStatus) {
        job.setStatus(newStatus);
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(job.getPath()))) {
            GSON.toJson(job, writer);
            writer.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
