package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadWorkshopVenue;
import me.noynto.eosa.shared.WorkshopVenueId;

import java.util.HashMap;
import java.util.Map;

public record GetAdminWorkshopVenueHandler(ReadWorkshopVenue readWorkshopVenue) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            var workshopVenue = readWorkshopVenue.handle(new ReadWorkshopVenue.Command(new WorkshopVenueId(ctx.pathParam("id"))));
            boolean hasImage = workshopVenue.getImageId() != null;
            Map<String, Object> model = new HashMap<>();
            model.put("title", workshopVenue.getName());
            model.put("workshopVenueId", workshopVenue.getId().value());
            model.put("name", workshopVenue.getName());
            model.put("description", workshopVenue.getDescription() != null ? workshopVenue.getDescription() : "");
            model.put("address", workshopVenue.getAddress());
            model.put("postalCode", workshopVenue.getPostalCode());
            model.put("city", workshopVenue.getCity());
            model.put("linkUrl", workshopVenue.getLinkUrl() != null ? workshopVenue.getLinkUrl() : "");
            model.put("defaultCapacity", workshopVenue.getDefaultCapacity());
            model.put("defaultDurationMinutes", workshopVenue.getDefaultDurationMinutes());
            model.put("archived", workshopVenue.isArchived());
            model.put("hasImage", hasImage);
            model.put("imageId", hasImage ? workshopVenue.getImageId().value() : "");
            ctx.render("admin/workshop-venue.mustache", model);
        } catch (RuntimeException e) {
            ctx.status(404);
        }
    }

}
