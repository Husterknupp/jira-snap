package de.bschandera.jiraversioncleaner.resources;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.bschandera.jiraversioncleaner.core.Job;
import de.bschandera.jiraversioncleaner.core.Version;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/versions")
public class CleanerResource {
    private static final java.nio.file.Path JOBS_PATH = Paths.get("./jobs");
    private static final java.nio.file.Path VERSIONS_FILE_PATH = Paths.get("versions.json");

    @GET
    public CleanerView getCleanerView(@Context HttpServletRequest request) throws InterruptedException, IOException {
        java.nio.file.Path jobFileName = JOBS_PATH.resolve(new Date().getTime() + ".json");
        List<String> configuredComponents = Arrays.asList("component-a", "component-b"); // todo configure
        Writer writer = java.nio.file.Files.newBufferedWriter(jobFileName);
        new Gson().toJson(Job.openPollingJob(configuredComponents), writer);
        writer.close();

        Job pollJob;
        int tries = 0;
        do {
            BufferedReader reader = Files.newBufferedReader(jobFileName);
            pollJob = new Gson().fromJson(reader, Job.class);
            reader.close();
            tries++;
            Thread.sleep(500);
        } while (pollJob.getStatus().equals("open") && tries < 10);

        if (pollJob.getStatus().equals("done")) {
            System.out.println("[INFO] polling job was done");
            List<Version> pollingResult = readVersionsFromFile();
            return new CleanerView(configuredComponents, configuredComponents.stream().collect(Collectors
                    .toMap(Function.<String>identity(),
                            component -> findVersionsForComponent(component, pollingResult))));
        } else {
            System.out.println("[WARN] could not find polling job done: " + jobFileName);
            return new CleanerView(configuredComponents, Collections.emptyMap());
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
    @PUT
    public Response triggerUpdateForVersion(@PathParam("versionName") String versionName,
                                            @QueryParam("isReleased") boolean isReleased) throws IOException {
        if (!isReleased) {
            return Response.notModified().build();
        }

        readVersionsFromFile().stream()
                .filter(version -> version.getName().equalsIgnoreCase(versionName))
                .findFirst()
                .ifPresent(version -> {
                    java.nio.file.Path jobFileName = JOBS_PATH.resolve(new Date().getTime() + ".json");
                    Writer writer = null;
                    try {
                        writer = Files.newBufferedWriter(jobFileName);
                        version.setIsReleased(true);
                        version.setReleaseDate(new Date());
                        new Gson().toJson(Job.openUpdateJob(version), writer);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } finally {
                        try {
                            if (writer != null) {
                                writer.close();
                            }
                        } catch (IOException e) {
                            System.out.println(e.getMessage());
                        }
                    }
                    System.out.println("[INFO] created job for version update of " + versionName);
                    System.out.println("[INFO] job name" + jobFileName);
                });

        return Response.ok().build();
    }

}
