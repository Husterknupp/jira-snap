package de.bschandera.jiraversioncleaner.resources;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.bschandera.jiraversioncleaner.core.Job;
import de.bschandera.jiraversioncleaner.core.Version;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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
        java.nio.file.Path jobFileName = JOBS_PATH.resolve(new Date().getTime() + ".json");
        Writer writer = java.nio.file.Files.newBufferedWriter(jobFileName);

        new Gson().toJson(Job.openPollingJob(componentsToPollFor), writer);
        writer.close();

        Job pollJob;
        int tries = 0;
        do {
            BufferedReader reader = Files.newBufferedReader(jobFileName);
            pollJob = new Gson().fromJson(reader, Job.class);
            reader.close();
            tries++;
            Thread.sleep(500);
        } while (pollJob.getStatus().equalsIgnoreCase("open") && tries < 10);
        System.out.println("[INFO] polling job was done");
        if (pollJob.getStatus().equalsIgnoreCase("failed")) {
            System.out.println("[ERROR] polling job failed");
        }
        if (Files.exists(VERSIONS_FILE_PATH)) {
            List<Version> pollingResult = readVersionsFromFile();
            return new CleanerView(componentsToPollFor, componentsToPollFor.stream().collect(Collectors
                    .toMap(Function.<String>identity(),
                            component -> findVersionsForComponent(component, pollingResult))));
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

    @Path("{versionName}")
    @POST
    public SimpleBackButtonView updateVersion(@PathParam("versionName") String versionName,
                                              @FormParam("releaseDate") String releaseDateString) throws IOException, InterruptedException {
        final Date releaseDate;
        try {
            releaseDate = DateFormat.getInstance().parse(releaseDateString);
        } catch (ParseException e) {
            System.out.println("[ERROR] " + e.getMessage());
            return new SimpleBackButtonView();
        }

        Optional<Version> version = readVersionsFromFile().stream()
                .filter(v -> v.getName().equalsIgnoreCase(versionName))
                .findFirst();
        if (!version.isPresent()) {
            System.out.println("[WARN] Could not find version of name '" + versionName + "'");
            return new SimpleBackButtonView();
        }

        java.nio.file.Path jobFileName = JOBS_PATH.resolve(new Date().getTime() + ".json");
        Writer writer = null;
        try {
            writer = Files.newBufferedWriter(jobFileName);
            version.get().setIsReleased(true);
            version.get().setReleaseDate(releaseDate);
            new Gson().toJson(Job.openUpdateJob(version.get()), writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                System.out.println("[ERROR] " + e.getMessage());
            }
        }
        System.out.println("[INFO] created job for version update of " + versionName);
        System.out.println("[INFO] job name" + jobFileName);
        return new SimpleBackButtonView();
    }

}
