package de.bschandera.jiraversioncleaner.resources;

import io.dropwizard.views.View;

import java.util.List;
import java.util.Map;

public class CleanerView extends View {
    private List<String> configuredComponents;
    private Map<String, List<String>> versions;

    public CleanerView(List<String> configuredComponents, Map<String, List<String>> versions) {
        super("cleaner.ftl");
        this.configuredComponents = configuredComponents;
        this.versions = versions;
    }

    public List<String> getConfiguredComponents() {
        return configuredComponents;
    }

    public Map<String, List<String>> getVersions() {
        return versions;
    }
}

