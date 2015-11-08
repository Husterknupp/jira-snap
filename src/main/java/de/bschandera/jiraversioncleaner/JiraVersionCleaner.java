package de.bschandera.jiraversioncleaner;

import de.bschandera.jiraversioncleaner.resources.CleanerResource;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;

public class JiraVersionCleaner extends Application<JiraVersionCleanerConfiguration> {

    public static void main(String[] args) throws Exception {
        new JiraVersionCleaner().run(args);
    }

    @Override
    public String getName() {
        return "JiraVersionCleaner";
    }

    @Override
    public void run(JiraVersionCleanerConfiguration configuration, Environment environment) throws Exception {
        environment.jersey().register(new CleanerResource(configuration.getComponentsToPollFor()));
    }

    @Override
    public void initialize(Bootstrap<JiraVersionCleanerConfiguration> bootstrap) {
        super.initialize(bootstrap);
        bootstrap.addBundle(new ViewBundle<>());
    }
}
