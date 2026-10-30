package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.UpdateWorkshopSession;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.shared.WorkshopVenueId;

public record UpdateWorkshopSessionHandler(UpdateWorkshopSession updateWorkshopSession) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            updateWorkshopSession.handle(new UpdateWorkshopSession.Command(
                    new WorkshopSessionId(ctx.pathParam("id")),
                    new WorkshopVenueId(ctx.formParam("venueId")),
                    WorkshopSessionFormat.parseInputs(ctx.formParam("date"), ctx.formParam("time")),
                    WorkshopSessionFormat.parseInteger(ctx.formParam("durationMinutes")),
                    WorkshopSessionFormat.parseInteger(ctx.formParam("capacity"))
            ));
            // La date, l'heure de fin et l'ouverture des réservations affichées dépendent des valeurs saisies.
            ctx.header("HX-Refresh", "true");
            ctx.html("<span class=\"text-success text-xs\">Sauvegardé</span>");
        } catch (RuntimeException e) {
            ctx.status(422).html("<span class=\"text-red-600 text-xs\">" + e.getMessage() + "</span>");
        }
    }

}
