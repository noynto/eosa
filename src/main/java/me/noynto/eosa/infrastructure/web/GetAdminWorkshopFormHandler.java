package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadWorkshopVenues;

import java.util.HashMap;
import java.util.Map;

/**
 * Fragment de l'administration : le contenu de la fenêtre « Nouvelle session », chargé à son ouverture avec les
 * lieux actifs et leurs valeurs par défaut.
 */
public record GetAdminWorkshopFormHandler(ReadWorkshopVenues readWorkshopVenues) implements Handler {

    @Override
    public void handle(Context ctx) {
        var venues = readWorkshopVenues.handle().venues().stream()
                .filter(venue -> !venue.isArchived())
                .map(venue -> {
                    Map<String, Object> option = new HashMap<>();
                    option.put("id", venue.getId().value());
                    option.put("name", venue.getName());
                    option.put("city", venue.getCity());
                    option.put("defaultCapacity", venue.getDefaultCapacity());
                    option.put("defaultDurationMinutes", venue.getDefaultDurationMinutes());
                    return option;
                })
                .toList();

        Map<String, Object> model = new HashMap<>();
        model.put("venues", venues);
        model.put("hasVenues", !venues.isEmpty());
        model.put("firstVenueCapacity", venues.isEmpty() ? "" : venues.getFirst().get("defaultCapacity"));
        model.put("firstVenueDuration", venues.isEmpty() ? "" : venues.getFirst().get("defaultDurationMinutes"));
        ctx.render("admin/partials/workshop-create-form.mustache", model);
    }

}
