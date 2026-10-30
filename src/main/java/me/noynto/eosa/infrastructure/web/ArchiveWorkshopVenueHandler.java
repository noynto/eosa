package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ArchiveWorkshopVenue;
import me.noynto.eosa.shared.WorkshopVenueId;

public record ArchiveWorkshopVenueHandler(ArchiveWorkshopVenue archiveWorkshopVenue) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            archiveWorkshopVenue.handle(new ArchiveWorkshopVenue.Command(new WorkshopVenueId(ctx.pathParam("id"))));
            ctx.header("HX-Redirect", "/workshop-venues");
            ctx.status(200);
        } catch (RuntimeException e) {
            ctx.status(422).html("<span class=\"text-red-600 text-xs\">" + e.getMessage() + "</span>");
        }
    }

}
