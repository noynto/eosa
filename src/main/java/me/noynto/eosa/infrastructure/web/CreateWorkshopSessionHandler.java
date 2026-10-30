package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.CreateWorkshopSession;
import me.noynto.eosa.shared.WorkshopVenueId;

public record CreateWorkshopSessionHandler(CreateWorkshopSession createWorkshopSession) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            var workshopSession = createWorkshopSession.handle(new CreateWorkshopSession.Command(
                    new WorkshopVenueId(ctx.formParam("venueId")),
                    WorkshopSessionFormat.parseInputs(ctx.formParam("date"), ctx.formParam("time")),
                    WorkshopSessionFormat.parseInteger(ctx.formParam("durationMinutes")),
                    WorkshopSessionFormat.parseInteger(ctx.formParam("capacity"))
            ));
            ctx.header("HX-Redirect", "/workshops/" + workshopSession.getId().value());
            ctx.status(200);
        } catch (RuntimeException e) {
            ctx.status(422).html("<span class=\"text-red-600 text-xs\">" + e.getMessage() + "</span>");
        }
    }

}
