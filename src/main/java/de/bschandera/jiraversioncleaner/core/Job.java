package de.bschandera.jiraversioncleaner.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Job {
    private String status;
    private String type;
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

    public static Job openPollingJob(List<String> componentsWanted) {
        Job job = new Job();
        job.setStatus("open");
        job.setType("poll");
        job.setComponentsWanted(componentsWanted);
        return job;
    }

    public static Job openUpdateJob(Version firstVersion, Version... moreVersions) {
        Job job = new Job();
        job.setStatus("open");
        job.setType("update");
        if (moreVersions.length != 0) {
            moreVersions[moreVersions.length] = firstVersion;
            job.setUpdatedVersions(Arrays.asList(moreVersions));
        } else {
            job.setUpdatedVersions(Collections.singletonList(firstVersion));
        }
        return job;
    }
}
