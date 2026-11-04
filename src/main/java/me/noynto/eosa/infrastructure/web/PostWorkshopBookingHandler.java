package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.RequestWorkshopBooking;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopBooking;

import java.util.Map;

/**
 * Demande de réservation depuis la page d'une session. La réponse est un fragment htmx (voir
 * {@link WorkshopFormResponse}) : la confirmation « Vérifiez votre boîte mail » remplace le formulaire.
 */
public record PostWorkshopBookingHandler(
        RequestWorkshopBooking requestWorkshopBooking,
        RequestRateLimiter requestRateLimiter,
        String baseUrl
) implements Handler {

    /**
     * Champ caché aux humains (hors écran, sans tabulation) : seuls les robots le remplissent.
     */
    private static final String HONEYPOT = "website";

    @Override
    public void handle(Context ctx) {
        String firstName = valueOf(ctx, "firstName");
        String email = valueOf(ctx, "email");

        // Robot : on fait comme si la demande était partie, sans rien enregistrer ni envoyer.
        String honeypot = ctx.formParam(HONEYPOT);
        if (honeypot != null && !honeypot.isBlank()) {
            requested(ctx, firstName, email);
            return;
        }
        if (!requestRateLimiter.tryAcquire(RequestRateLimiter.clientOf(ctx))) {
            WorkshopFormResponse.error(ctx, baseUrl, "Trop de demandes depuis votre connexion. Réessayez dans une heure, "
                    + "ou écrivez-nous sur Instagram (@eosa.bijoux).");
            return;
        }

        WorkshopBooking workshopBooking;
        try {
            workshopBooking = requestWorkshopBooking.handle(new RequestWorkshopBooking.Command(
                    new WorkshopSessionId(ctx.pathParam("id")),
                    firstName,
                    valueOf(ctx, "lastName"),
                    email,
                    valueOf(ctx, "phone"),
                    ctx.formParam("acceptedConditions") != null
            )).booking();
        } catch (RuntimeException e) {
            WorkshopFormResponse.error(ctx, baseUrl, e.getMessage());
            return;
        }
        requested(ctx, workshopBooking.getFirstName(), workshopBooking.getEmail());
    }

    private void requested(Context ctx, String firstName, String email) {
        if (WorkshopFormResponse.isHtmx(ctx)) {
            ctx.render("partials/workshop-booking-requested.mustache", Map.of(
                    "sessionId", ctx.pathParam("id"),
                    "firstName", firstName,
                    "email", email,
                    "confirmationDelayMinutes", WorkshopBooking.CONFIRMATION_DELAY.toMinutes()
            ));
            return;
        }
        WorkshopFormResponse.page(ctx, baseUrl, "Vérifiez votre boîte mail",
                "Merci " + firstName + " ! Nous venons d'envoyer un e-mail à " + email + ". Votre place est retenue pendant "
                        + WorkshopBooking.CONFIRMATION_DELAY.toMinutes() + " minutes : cliquez sur « Confirmer ma place » dans l'e-mail pour la garder.",
                false);
    }

    private static String valueOf(Context ctx, String field) {
        String value = ctx.formParam(field);
        return value != null ? value : "";
    }

}
