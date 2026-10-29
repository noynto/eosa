package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.CancelWorkshopBookingByAdmin;
import me.noynto.eosa.shared.WorkshopBookingId;
import me.noynto.eosa.shared.WorkshopSessionId;

public record CancelWorkshopBookingByAdminHandler(CancelWorkshopBookingByAdmin cancelWorkshopBookingByAdmin) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            cancelWorkshopBookingByAdmin.handle(new CancelWorkshopBookingByAdmin.Command(
                    new WorkshopSessionId(ctx.pathParam("id")),
                    new WorkshopBookingId(ctx.pathParam("bookingId"))
            ));
            ctx.header("HX-Refresh", "true");
            ctx.status(200);
        } catch (RuntimeException e) {
            ctx.status(422).html("<span class=\"text-red-600 text-xs\">" + e.getMessage() + "</span>");
        }
    }

}
