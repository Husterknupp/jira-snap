package de.bschandera;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class JsonUtil {
    private static final Gson GSON = new Gson();
    private static final Path ROOT_PATH = Paths.get(".");

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
        DirectoryStream<Path> jobFilesStream;
        Path jobsPath = ROOT_PATH.resolve("jobs");
        try {
            jobFilesStream = Files.newDirectoryStream(jobsPath);
        } catch (IOException e) {
            System.out.println("[ERROR] I/O problem with path " + jobsPath);
            throw new UncheckedIOException(e);
        }
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
            } catch (IOException e) {
                System.out.println("[ERROR] I/O problem in file " + aJobFilesStream);
                throw new UncheckedIOException(e);
            }

            if (job.getStatus().equalsIgnoreCase("open") && job.getType().equalsIgnoreCase(jobType)) {
                job.setPath(aJobFilesStream.toString());
                System.out.println("[INFO] found open job: " + jobType + " " + job.getPath());
                result = Optional.of(job);
                break;
            }
        }

        try {
            jobFilesStream.close();
        } catch (IOException e) {
            System.out.println("[ERROR] I/O problem in file " + jobFilesStream);
            throw new UncheckedIOException(e);
        }
        return result;
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
