package de.bschandera.jiraversioncleaner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.dropwizard.Configuration;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraVersionCleanerConfiguration extends Configuration {
    List<String> componentsToPollFor;

    public List<String> getComponentsToPollFor() {
        return componentsToPollFor;
    }
}
