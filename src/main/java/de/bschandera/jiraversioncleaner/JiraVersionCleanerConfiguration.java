package de.bschandera.jiraversioncleaner;

import io.dropwizard.Configuration;

import java.util.List;

public class JiraVersionCleanerConfiguration extends Configuration {
    List<String> componentsToPollFor;

    public List<String> getComponentsToPollFor() {
        return componentsToPollFor;
    }
}
