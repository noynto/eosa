package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.infrastructure.fetch.photon.resource.PhotonApiResource;

import java.util.List;
import java.util.Map;

public record GetPhysicalAddressSuggestionsHandler(PhotonApiResource photon) implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        String query = ctx.queryParam("q");
        if (query == null || query.isBlank() || query.length() < 3) {
            ctx.render("checkout/physical-address-suggestions.jte", Map.of("features", List.of()));
            return;
        }
        var features = photon.get(query, 5L, "fr").toList();
        ctx.render("checkout/physical-address-suggestions.jte", Map.of("features", features));
    }

}