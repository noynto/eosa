package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadWorkshopVenues;

import java.util.HashMap;
import java.util.Map;

/**
 * Fragment de la page d'une session pour l'administration : les lieux proposés par la liste de choix du lieu,
 * c'est-à-dire les lieux actifs, plus le lieu actuel de la session même s'il a été archivé depuis
 * (paramètre {@code selected}).
 */
public record GetAdminWorkshopVenueOptionsHandler(ReadWorkshopVenues readWorkshopVenues) implements Handler {

    @Override
    public void handle(Context ctx) {
        String selected = ctx.queryParam("selected");
        var options = readWorkshopVenues.handle().stream()
                .filter(venue -> !venue.isArchived() || venue.getId().value().equals(selected))
                .map(venue -> {
                    Map<String, Object> option = new HashMap<>();
                    option.put("id", venue.getId().value());
                    option.put("name", venue.getName());
                    option.put("city", venue.getCity());
                    option.put("selected", venue.getId().value().equals(selected));
                    return option;
                })
                .toList();
        ctx.render("admin/partials/workshop-venue-options.mustache", Map.of("venues", options));
    }

}
