package com.kruize.optimizer.resource;

import com.kruize.optimizer.model.WebhookPayload;
import com.kruize.optimizer.service.WorkloadAutomatorService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/webhook")
public class WebhookResource {

    @Inject
    WorkloadAutomatorService workloadAutomatorService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response receiveWebhook(List<WebhookPayload> payload) {
        workloadAutomatorService.handleWebhook(payload);
        return Response.ok().build();
    }
}
