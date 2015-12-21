package de.bschandera;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

public class JsonUtilTest {
    private static final Path ROOT_PATH = Paths.get(".");

    @Before
    public void cleanJobsFolderAndVersionsFile() throws IOException {
        Files.deleteIfExists(ROOT_PATH.resolve("config.json"));
    }

    @AfterClass
    public static void cleanUpAfterwards() throws IOException {
        Files.deleteIfExists(ROOT_PATH.resolve("config.json"));
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

}
