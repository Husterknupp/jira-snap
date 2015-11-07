package de.bschandera.jiraversioncleaner.resources;

import de.bschandera.jiraversioncleaner.core.Version;
import io.dropwizard.views.View;

import java.util.List;
import java.util.Map;

public class CleanerView extends View {
    private List<String> configuredComponents;
    private Map<String, List<Version>> versions;

    public CleanerView(List<String> configuredComponents, Map<String, List<Version>> versions) {
        super("cleaner.ftl");
        this.configuredComponents = configuredComponents;
        this.versions = versions;
    }

    public List<String> getConfiguredComponents() {
        return configuredComponents;
    }

    public Map<String, List<Version>> getVersions() {
        return versions;
    }
}

