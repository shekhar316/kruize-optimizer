package com.kruize.optimizer.resource;

import com.kruize.optimizer.client.KruizeClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/experiments")
public class ExperimentResource {

    @Inject
    @RestClient
    KruizeClient kruizeClient;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String listExperiments() {
        return kruizeClient.listExperiments();
    }
}
