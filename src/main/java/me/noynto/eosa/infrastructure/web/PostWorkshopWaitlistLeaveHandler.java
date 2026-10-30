package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import me.noynto.eosa.application.LeaveWorkshopWaitlist;

/**
 * Bouton « Quitter la liste d'attente » : désinscrit, puis réaffiche la page du lien.
 */
public record PostWorkshopWaitlistLeaveHandler(
        LeaveWorkshopWaitlist leaveWorkshopWaitlist
) implements Handler {

    @Override
    public void handle(Context ctx) {
        String token = ctx.pathParam("token");
        try {
            leaveWorkshopWaitlist.handle(new LeaveWorkshopWaitlist.Command(token));
        } catch (RuntimeException e) {
            ctx.status(404);
            return;
        }
        ctx.redirect("/workshops/waitlist/" + token + "/leave", HttpStatus.SEE_OTHER);
    }

}
