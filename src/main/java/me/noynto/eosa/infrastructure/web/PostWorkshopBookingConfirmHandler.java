package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import me.noynto.eosa.application.ConfirmWorkshopBooking;

/**
 * Bouton « Confirmer ma place » : confirme, puis réaffiche la page du lien avec le nouvel état.
 */
public record PostWorkshopBookingConfirmHandler(
        ConfirmWorkshopBooking confirmWorkshopBooking
) implements Handler {

    @Override
    public void handle(Context ctx) {
        String token = ctx.pathParam("token");
        try {
            confirmWorkshopBooking.handle(new ConfirmWorkshopBooking.Command(token));
        } catch (RuntimeException e) {
            ctx.status(404);
            return;
        }
        ctx.redirect("/workshops/bookings/" + token + "/confirm", HttpStatus.SEE_OTHER);
    }

}
