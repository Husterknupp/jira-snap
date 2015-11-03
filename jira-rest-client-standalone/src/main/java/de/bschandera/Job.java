package de.bschandera;

import com.atlassian.jira.rest.client.domain.Version;

import java.util.List;

public class Job {
    private String status;
    private String type;
    private String path;
    private List<String> componentsWanted;
    private List<Version> updatedVersions;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getComponentsWanted() {
        return componentsWanted;
    }

    public void setComponentsWanted(List<String> componentsWanted) {
        this.componentsWanted = componentsWanted;
    }

    public List<Version> getUpdatedVersions() {
        return updatedVersions;
    }

    public void setUpdatedVersions(List<Version> updatedVersions) {
        this.updatedVersions = updatedVersions;
    }
}
