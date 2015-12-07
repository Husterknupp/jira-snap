package de.bschandera.jiraversioncleaner.resources;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.bschandera.jiraversioncleaner.core.Job;
import de.bschandera.jiraversioncleaner.core.Version;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/versions")
public class CleanerResource {
    private static final java.nio.file.Path JOBS_PATH = Paths.get("./jobs");
    private static final java.nio.file.Path VERSIONS_FILE_PATH = Paths.get("versions.json");

    private final List<String> componentsToPollFor;

    public CleanerResource(List<String> componentsToPollFor) {
        this.componentsToPollFor = componentsToPollFor;
    }

    @GET
    public CleanerView getCleanerView() throws InterruptedException, IOException {
        if (Files.exists(VERSIONS_FILE_PATH)) {
            List<Version> versions = readVersionsFromFile();
            return new CleanerView(componentsToPollFor, componentsToPollFor.stream().collect(Collectors
                    .toMap(Function.<String>identity(),
                            component -> findVersionsForComponent(component, versions))));
        } else {
            return new CleanerView(componentsToPollFor, Collections.emptyMap());
        }
    }

    private List<Version> readVersionsFromFile() throws IOException {
        Type srcTypeVersionList = new TypeToken<List<Version>>() {
        }.getType();
        BufferedReader reader = Files.newBufferedReader(VERSIONS_FILE_PATH);
        List<Version> pollingResult = new Gson().fromJson(reader, srcTypeVersionList);
        reader.close();
        return pollingResult;
    }

    private List<Version> findVersionsForComponent(String component, List<Version> unreleasedVersions) {
        return unreleasedVersions.stream()
                .filter(version -> version.getName().startsWith(component)).collect(Collectors.toList());
    }

    @POST
    public CleanerView updateVersion(@FormParam("versionName") String versionName,
                                     @FormParam("releaseDate") String releaseDateString,
                                     @DefaultValue("false") @FormParam("refreshVersions") Boolean refreshWanted)
            throws IOException, InterruptedException {
        if (releaseDateString != null && versionName != null) {
            final Date releaseDate;
            try {
                releaseDate = DateFormat.getInstance().parse(releaseDateString);
            } catch (ParseException e) {
                System.out.println("[ERROR] " + e.getMessage());
                return getCleanerView().withMessage("Nope. Your date format is bad.");
            }

            Optional<Version> version = readVersionsFromFile().stream()
                    .filter(v -> v.getName().equalsIgnoreCase(versionName))
                    .findFirst();
            if (!version.isPresent()) {
                System.out.println("[WARN] Could not find version of name '" + versionName + "'");
                return getCleanerView().withMessage("Ain't no version of this name, dude.");
            }

            java.nio.file.Path updateJobName = JOBS_PATH.resolve(new Date().getTime() + ".json");
            Optional<String> error = putUpdateJob(releaseDate, version.get(), updateJobName);
            if (error.isPresent()) {
                return getCleanerView().withMessage(error.get());
            }
            error = checkIfUpdateIsDone(updateJobName);
            if (error.isPresent()) {
                return getCleanerView().withMessage(error.get());
            }
        }

        if (!refreshWanted) {
            if (releaseDateString != null && versionName != null) {
                return getCleanerView().withMessage("Update was successful. Please refresh versions.");
            } else {
                return getCleanerView();
            }
        }

        java.nio.file.Path pollJobName = JOBS_PATH.resolve(new Date().getTime() + ".json");
        putPollJob(pollJobName);

        Optional<String> error = checkIfPollingIsDone(pollJobName);
        String message;
        if (error.isPresent()) {
            message = error.get();
        } else {
            if (versionName == null) {
                message = "Versions are up-to-date.";
            } else if (readVersionsFromFile().stream()
                    .filter(v -> v.getName().equalsIgnoreCase(versionName))
                    .findFirst().isPresent()) {
                message = "Version was not released.";
            } else {
                message = "Aww yiss! Version was released.";
            }
        }
        return getCleanerView().withMessage(message);
    }

    private Optional<String> putUpdateJob(Date releaseDate, Version version, java.nio.file.Path updateJobName)
            throws InterruptedException, IOException {
        version.setIsReleased(true);
        version.setReleaseDate(releaseDate);
        Job job = Job.openUpdateJob(version);
        Optional<String> error = writeToJson(updateJobName, job);
        if (error.isPresent()) {
            return error;
        }
        System.out.println("[INFO] created job for version update of " + version.getName());
        System.out.println("[INFO] job name" + updateJobName);
        return Optional.empty();
    }

    private Optional<String> checkIfUpdateIsDone(java.nio.file.Path updateJobName)
            throws IOException, InterruptedException {
        Job updateJob = waitForJobStateChange(updateJobName);
        if (updateJob.getStatus().equalsIgnoreCase("done")) {
            System.out.println("[INFO] update job was done");
            return Optional.empty();
        } else if (updateJob.getStatus().equalsIgnoreCase("failed")) {
            System.out.println("[ERROR] update job " + updateJobName + " failed");
            return Optional.of("Nope. Version update failed.");
        } else if (updateJob.getStatus().equalsIgnoreCase("open")) {
            return Optional.of("Nope. Jira rest standalone not running. Job still open.");
        } else {
            return Optional.of("Job status not known: " + updateJob.getStatus());
        }
    }

    private void putPollJob(java.nio.file.Path pollJobName) throws IOException {
        Job job = Job.openPollingJob(componentsToPollFor);
        writeToJson(pollJobName, job);
    }

    private Optional<String> writeToJson(java.nio.file.Path jobName, Job job) {
        Writer writer = null;
        try {
            writer = Files.newBufferedWriter(jobName);
            new Gson().toJson(job, writer);
            return Optional.empty();
        } catch (IOException e) {
            return Optional.of("I/O made a problem. " + e.getMessage());
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                System.out.println("[ERROR] " + e.getMessage());
            }
        }
    }

    private Optional<String> checkIfPollingIsDone(java.nio.file.Path pollJobName)
            throws IOException, InterruptedException {
        Job pollJob = waitForJobStateChange(pollJobName);
        if (pollJob.getStatus().equalsIgnoreCase("done")) {
            System.out.println("[INFO] polling job was done");
            return Optional.empty();
        } else if (pollJob.getStatus().equalsIgnoreCase("failed")) {
            System.out.println("[ERROR] poll job " + pollJobName + " failed");
            return Optional.of("Version list is not up-to-date.");
        } else if (pollJob.getStatus().equalsIgnoreCase("open")) {
            return Optional.of("Nope. Jira rest standalone not running. Job still open.");
        } else {
            return Optional.of("Job status not known: " + pollJob.getStatus());
        }
    }

    private Job waitForJobStateChange(java.nio.file.Path jobName) throws IOException, InterruptedException {
        Job job;
        int tries = 0;
        do {
            BufferedReader reader = Files.newBufferedReader(jobName);
            job = new Gson().fromJson(reader, Job.class);
            reader.close();
            tries++;
            Thread.sleep(500);
        } while (job.getStatus().equalsIgnoreCase("open") && tries < 30);
        return job;
    }

}
