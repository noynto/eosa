package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.RemindWorkshopWaitlist;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopWaitlistEntry;

import java.util.Map;

/**
 * Relance de la liste d'attente d'une session : renvoie le message affiché à côté du bouton.
 */
public record RemindWorkshopWaitlistHandler(RemindWorkshopWaitlist remindWorkshopWaitlist) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            var result = remindWorkshopWaitlist.handle(new RemindWorkshopWaitlist.Command(new WorkshopSessionId(ctx.pathParam("id"))));
            ctx.render("admin/partials/workshop-waitlist-reminded.mustache", Map.of(
                    "message", message(result)
            ));
        } catch (RuntimeException e) {
            InlineError.render(ctx, e);
        }
    }

    private static String message(RemindWorkshopWaitlist.Result result) {
        String notified = result.notified() == 0 ? "Personne n'a été prévenue"
                : result.notified() == 1 ? "1 personne prévenue" : result.notified() + " personnes prévenues";
        if (result.skipped() == 0) {
            return notified + ".";
        }
        long hours = WorkshopWaitlistEntry.MIN_DELAY_BETWEEN_EMAILS.toHours();
        String skipped = result.skipped() == 1 ? "1 déjà prévenue" : result.skipped() + " déjà prévenues";
        return notified + ", " + skipped + " il y a moins de " + hours + " h.";
    }

}
