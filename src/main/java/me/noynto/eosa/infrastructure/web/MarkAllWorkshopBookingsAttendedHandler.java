package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.MarkAllWorkshopBookingsAttended;
import me.noynto.eosa.shared.WorkshopSessionId;

public record MarkAllWorkshopBookingsAttendedHandler(MarkAllWorkshopBookingsAttended markAllWorkshopBookingsAttended) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            markAllWorkshopBookingsAttended.handle(new MarkAllWorkshopBookingsAttended.Command(new WorkshopSessionId(ctx.pathParam("id"))));
            ctx.header("HX-Refresh", "true");
            ctx.status(200);
        } catch (RuntimeException e) {
            ctx.status(422).html("<span class=\"text-red-600 text-xs\">" + e.getMessage() + "</span>");
        }
    }

}
