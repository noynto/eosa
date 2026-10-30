package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.UpdateWorkshopVenue;
import me.noynto.eosa.shared.WorkshopVenueId;

public record UpdateWorkshopVenueHandler(UpdateWorkshopVenue updateWorkshopVenue) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            updateWorkshopVenue.handle(new UpdateWorkshopVenue.Command(
                    new WorkshopVenueId(ctx.pathParam("id")),
                    ctx.formParam("name"),
                    ctx.formParam("description"),
                    ctx.formParam("address"),
                    ctx.formParam("postalCode"),
                    ctx.formParam("city"),
                    ctx.formParam("linkUrl")
            ));
            ctx.html("<span class=\"text-success text-xs\">Sauvegardé</span>");
        } catch (RuntimeException e) {
            ctx.status(422).html("<span class=\"text-red-600 text-xs\">" + e.getMessage() + "</span>");
        }
    }

}
