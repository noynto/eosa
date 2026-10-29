package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import me.noynto.eosa.application.CancelWorkshopBookingByParticipant;

/**
 * Bouton « Confirmer l'annulation » : annule si c'est encore possible, puis réaffiche la page du lien
 * avec le nouvel état (annulée, ou trop tard).
 */
public record PostWorkshopBookingCancelHandler(
        CancelWorkshopBookingByParticipant cancelWorkshopBookingByParticipant
) implements Handler {

    @Override
    public void handle(Context ctx) {
        String token = ctx.pathParam("token");
        try {
            cancelWorkshopBookingByParticipant.handle(new CancelWorkshopBookingByParticipant.Command(token));
        } catch (RuntimeException e) {
            ctx.status(404);
            return;
        }
        ctx.redirect("/workshops/bookings/" + token + "/cancel", HttpStatus.SEE_OTHER);
    }

}
