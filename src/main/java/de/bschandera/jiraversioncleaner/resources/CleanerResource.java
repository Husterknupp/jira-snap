package de.bschandera.jiraversioncleaner.resources;

import de.bschandera.jiraversioncleaner.core.Janitor;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Arrays;

@Path("/versions")
public class CleanerResource {

    @GET
    public CleanerView getCleanerView(@Context HttpServletRequest request) {
        Janitor janitor = new Janitor();
        return new CleanerView(Arrays.asList("component a", "component b"), janitor.getVersions());
    }

    @GET
    @Produces(value = "application/json")
    public Response getDeliveryTimeForOrderId() {
        return Response.serverError().build();
    }

}
