package de.bschandera;

import java.net.URI;
import java.util.Date;

public class Version {
    private URI self;
    private String name;
    private boolean isReleased;
    private Date releaseDate;
    private boolean isArchived;

    public URI getSelf() {
        return self;
    }

    public void setSelf(URI self) {
        this.self = self;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isReleased() {
        return isReleased;
    }

    public void setIsReleased(boolean isReleased) {
        this.isReleased = isReleased;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }

    public boolean isArchived() {
        return isArchived;
    }

    public void setIsArchived(boolean isArchived) {
        this.isArchived = isArchived;
    }

    public static Version ofJiraVersion(com.atlassian.jira.rest.client.domain.Version jiraVersion) {
        Version result = new Version();
        result.setIsReleased(jiraVersion.isReleased());
        result.setName(jiraVersion.getName());
        result.setReleaseDate(jiraVersion.getReleaseDate() != null ? new Date(jiraVersion.getReleaseDate().getMillis()) : null);
        result.setSelf(jiraVersion.getSelf());
        result.setIsArchived(jiraVersion.isArchived());
        return result;
    }
}
