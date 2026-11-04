package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.JoinWorkshopWaitlist;
import me.noynto.eosa.shared.WorkshopSessionId;

import java.util.Map;

/**
 * Inscription sur la liste d'attente depuis la page d'une session : avant l'ouverture des réservations,
 * ou quand la session est complète. La réponse est un fragment htmx (voir {@link WorkshopFormResponse}) qui
 * remplace le formulaire par la confirmation.
 */
public record PostWorkshopWaitlistHandler(
        JoinWorkshopWaitlist joinWorkshopWaitlist,
        RequestRateLimiter requestRateLimiter,
        String baseUrl
) implements Handler {

    /**
     * Champ caché aux humains : seuls les robots le remplissent.
     */
    private static final String HONEYPOT = "website";

    @Override
    public void handle(Context ctx) {
        String firstName = ctx.formParam("firstName") != null ? ctx.formParam("firstName") : "";
        String email = ctx.formParam("email") != null ? ctx.formParam("email") : "";

        // Robot : on fait comme si l'inscription avait réussi, sans rien enregistrer.
        String honeypot = ctx.formParam(HONEYPOT);
        if (honeypot != null && !honeypot.isBlank()) {
            joined(ctx, email);
            return;
        }
        if (!requestRateLimiter.tryAcquire(RequestRateLimiter.clientOf(ctx))) {
            WorkshopFormResponse.error(ctx, baseUrl, "Trop de demandes depuis votre connexion. Réessayez dans une heure, "
                    + "ou écrivez-nous sur Instagram (@eosa.bijoux).");
            return;
        }
        try {
            var entry = joinWorkshopWaitlist.handle(new JoinWorkshopWaitlist.Command(
                    new WorkshopSessionId(ctx.pathParam("id")),
                    firstName,
                    email
            )).entry();
            joined(ctx, entry.getEmail());
        } catch (RuntimeException e) {
            WorkshopFormResponse.error(ctx, baseUrl, e.getMessage());
        }
    }

    private void joined(Context ctx, String email) {
        String message = "C'est noté : nous vous écrirons à " + email + " dès que vous pourrez réserver.";
        if (WorkshopFormResponse.isHtmx(ctx)) {
            ctx.render("partials/workshop-waitlist-joined.mustache", Map.of("message", message));
            return;
        }
        WorkshopFormResponse.page(ctx, baseUrl, "C'est noté", message, false);
    }

}
