package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadWorkshopVenues;

import java.util.HashMap;
import java.util.Map;

public record GetAdminWorkshopVenuesHandler(ReadWorkshopVenues readWorkshopVenues) implements Handler {

    @Override
    public void handle(Context ctx) {
        var workshopVenues = readWorkshopVenues.handle();

        var rows = workshopVenues.stream().map(workshopVenue -> {
            boolean hasImage = workshopVenue.getImageId() != null;
            Map<String, Object> row = new HashMap<>();
            row.put("id", workshopVenue.getId().value());
            row.put("name", workshopVenue.getName());
            row.put("city", workshopVenue.getCity());
            row.put("defaultCapacity", workshopVenue.getDefaultCapacity());
            row.put("archived", workshopVenue.isArchived());
            row.put("hasImage", hasImage);
            row.put("imageId", hasImage ? workshopVenue.getImageId().value() : "");
            return row;
        }).toList();

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Eosa — Lieux d'atelier");
        model.put("workshopVenues", rows);
        model.put("hasWorkshopVenues", !rows.isEmpty());
        ctx.render("admin/workshop-venues.mustache", model);
    }

}
