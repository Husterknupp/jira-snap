package de.bschandera;

import java.net.URI;

public class Config {
    private URI jiraUrl;
    private String username;
    private String password;

    public URI getJiraUrl() {
        return jiraUrl;
    }

    public void setJiraUrl(URI jiraUrl) {
        this.jiraUrl = jiraUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
