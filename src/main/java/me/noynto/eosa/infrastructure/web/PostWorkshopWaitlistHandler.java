package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.JoinWorkshopWaitlist;
import me.noynto.eosa.shared.WorkshopSessionId;

import java.util.HashMap;
import java.util.Map;

/**
 * Inscription sur la liste d'attente depuis la page d'une session complète.
 */
public record PostWorkshopWaitlistHandler(
        JoinWorkshopWaitlist joinWorkshopWaitlist,
        GetWorkshopSessionHandler getWorkshopSessionHandler,
        RequestRateLimiter requestRateLimiter
) implements Handler {

    /**
     * Champ caché aux humains : seuls les robots le remplissent.
     */
    private static final String HONEYPOT = "website";

    @Override
    public void handle(Context ctx) {
        Map<String, String> values = new HashMap<>();
        values.put("firstName", ctx.formParam("firstName") != null ? ctx.formParam("firstName") : "");
        values.put("email", ctx.formParam("email") != null ? ctx.formParam("email") : "");
        String notice = "C'est noté : nous vous écrirons à " + values.get("email") + " dès qu'une place se libère.";

        // Robot : on fait comme si l'inscription avait réussi, sans rien enregistrer.
        String honeypot = ctx.formParam(HONEYPOT);
        if (honeypot != null && !honeypot.isBlank()) {
            getWorkshopSessionHandler.render(ctx, null, notice, Map.of());
            return;
        }
        if (!requestRateLimiter.tryAcquire(RequestRateLimiter.clientOf(ctx))) {
            getWorkshopSessionHandler.render(ctx, "Trop de demandes depuis votre connexion. Réessayez dans une heure, "
                    + "ou écrivez-nous sur Instagram (@eosa.bijoux).", null, values);
            return;
        }
        try {
            var entry = joinWorkshopWaitlist.handle(new JoinWorkshopWaitlist.Command(
                    new WorkshopSessionId(ctx.pathParam("id")),
                    values.get("firstName"),
                    values.get("email")
            ));
            getWorkshopSessionHandler.render(ctx, null,
                    "C'est noté : nous vous écrirons à " + entry.getEmail() + " dès qu'une place se libère.", Map.of());
        } catch (RuntimeException e) {
            getWorkshopSessionHandler.render(ctx, e.getMessage(), null, values);
        }
    }

}
