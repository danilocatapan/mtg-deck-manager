package com.mtg.controller;

import com.mtg.service.meta.CommanderMetaProfile;
import com.mtg.service.meta.ExternalMetaIngestionJob;
import com.mtg.service.meta.MetaProvider;
import com.mtg.service.meta.MetaSourceStatus;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;

@Path("/meta")
@Produces(MediaType.APPLICATION_JSON)
public class MetaController {

    @Inject
    MetaProvider metaProvider;

    @Inject
    ExternalMetaIngestionJob ingestionJob;

    @GET
    @Path("/sources")
    public Map<String, List<MetaSourceStatus>> sources() {
        return Map.of("sources", metaProvider.getSourceStatuses());
    }

    @POST
    @Path("/sync")
    public Map<String, List<MetaSourceStatus>> sync() {
        return Map.of("sources", ingestionJob.sync());
    }

    @GET
    @Path("/commanders/{commander}")
    public CommanderMetaProfile commander(
            @PathParam("commander") String commander,
            @QueryParam("bracket") String bracket,
            @QueryParam("sourceMode") String sourceMode
    ) {
        return metaProvider.getCommanderProfile(commander, bracket, sourceMode);
    }
}
