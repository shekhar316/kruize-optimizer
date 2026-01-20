package com.kruize.optimizer.client;

import com.kruize.optimizer.model.DatasourceListResponse;
import com.kruize.optimizer.model.KruizeProfile;

import com.kruize.optimizer.utils.OptimizerConstants;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import java.util.List;

@RegisterRestClient(configKey = "kruize-api")
public interface KruizeClient {

        @GET
        @Path(OptimizerConstants.KruizeClientConstants.LIST_DATASOURCE_ENDPOINT)
        @Produces(MediaType.APPLICATION_JSON)
        DatasourceListResponse getDatasources();

        @GET
        @Path(OptimizerConstants.KruizeClientConstants.LIST_METADATA_PROFILE_ENDPOINT)
        @Produces(MediaType.APPLICATION_JSON)
        List<KruizeProfile> getMetadataProfiles(
                        @jakarta.ws.rs.QueryParam(OptimizerConstants.KruizeClientConstants.VERBOSE) boolean verbose);

        @GET
        @Path(OptimizerConstants.KruizeClientConstants.LIST_METRIC_PROFILE_ENDPOINT)
        @Produces(MediaType.APPLICATION_JSON)
        List<KruizeProfile> getMetadataProfiles();

        @GET
        @Path(OptimizerConstants.KruizeClientConstants.LIST_METRIC_PROFILE_ENDPOINT)
        @Produces(MediaType.APPLICATION_JSON)
        List<KruizeProfile> getMetricProfiles(
                        @jakarta.ws.rs.QueryParam(OptimizerConstants.KruizeClientConstants.VERBOSE) boolean verbose);

        @GET
        @Path(OptimizerConstants.KruizeClientConstants.LIST_METRIC_PROFILE_ENDPOINT)
        @Produces(MediaType.APPLICATION_JSON)
        List<KruizeProfile> getMetricProfiles();

        @GET
        @Path(OptimizerConstants.KruizeClientConstants.LIST_LAYERS_ENDPOINT)
        @Produces(MediaType.APPLICATION_JSON)
        List<KruizeProfile> getLayers();

        @GET
        @Path("/listRuleSets")
        @Produces(MediaType.APPLICATION_JSON)
        List<KruizeProfile> getRuleSets();

        @GET
        @Path(OptimizerConstants.KruizeClientConstants.LIST_EXPERIMENTS_ENDPOINT)
        @Produces(MediaType.APPLICATION_JSON)
        String listExperiments();

        @POST
        @Path(OptimizerConstants.KruizeClientConstants.CREATE_METADATA_PROFILE_ENDPOINT)
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        String createMetadataProfile(Object profileDefinition);

        @POST
        @Path(OptimizerConstants.KruizeClientConstants.CREATE_METRIC_PROFILE_ENDPOINT)
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        String createMetricProfile(Object profileDefinition);

        @jakarta.ws.rs.PUT
        @Path(OptimizerConstants.KruizeClientConstants.UPDATE_METADATA_PROFILE_ENDPOINT)
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        String updateMetadataProfile(@jakarta.ws.rs.QueryParam("name") String name, Object profileDefinition);

        @jakarta.ws.rs.PUT
        @Path(OptimizerConstants.KruizeClientConstants.UPDATE_METRIC_PROFILE_ENDPOINT)
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        String updateMetricProfile(@jakarta.ws.rs.QueryParam("name") String name, Object profileDefinition);

        @POST
        @Path(OptimizerConstants.KruizeClientConstants.CREATE_LAYERS_ENDPOINT)
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        String createLayer(Object layerDefinition);

        @POST
        @Path("/createRuleSet")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        String createRuleSet(Object ruleSetDefinition);

        @POST
        @Path(OptimizerConstants.KruizeClientConstants.BULK_ENDPOINT)
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        String bulkCreateExperiments(Object bulkRequest);

        @GET
        @Path(OptimizerConstants.KruizeClientConstants.BULK_ENDPOINT)
        @Produces(MediaType.APPLICATION_JSON)
        String getBulkJobStatus(
                        @jakarta.ws.rs.QueryParam(OptimizerConstants.KruizeClientConstants.JOB_ID) String jobId);
}
