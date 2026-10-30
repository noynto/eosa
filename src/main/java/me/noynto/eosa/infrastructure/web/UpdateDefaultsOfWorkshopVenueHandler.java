package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.UpdateDefaultsOfWorkshopVenue;
import me.noynto.eosa.shared.WorkshopVenueId;

public record UpdateDefaultsOfWorkshopVenueHandler(UpdateDefaultsOfWorkshopVenue updateDefaultsOfWorkshopVenue) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            updateDefaultsOfWorkshopVenue.handle(new UpdateDefaultsOfWorkshopVenue.Command(
                    new WorkshopVenueId(ctx.pathParam("id")),
                    parseInteger(ctx.formParam("defaultCapacity")),
                    parseInteger(ctx.formParam("defaultDurationMinutes"))
            ));
            ctx.html("<span class=\"text-success text-xs\">Sauvegardé</span>");
        } catch (RuntimeException e) {
            ctx.status(422).html("<span class=\"text-red-600 text-xs\">" + e.getMessage() + "</span>");
        }
    }

    private static Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
