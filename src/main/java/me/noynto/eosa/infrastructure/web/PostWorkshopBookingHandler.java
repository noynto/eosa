package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadWorkshopSession;
import me.noynto.eosa.application.RequestWorkshopBooking;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopDates;

import java.util.HashMap;
import java.util.Map;

/**
 * Demande de réservation depuis la page d'une session.
 */
public record PostWorkshopBookingHandler(
        RequestWorkshopBooking requestWorkshopBooking,
        ReadWorkshopSession readWorkshopSession,
        GetWorkshopSessionHandler getWorkshopSessionHandler,
        RequestRateLimiter requestRateLimiter,
        String baseUrl
) implements Handler {

    /**
     * Champ caché aux humains (hors écran, sans tabulation) : seuls les robots le remplissent.
     */
    private static final String HONEYPOT = "website";

    @Override
    public void handle(Context ctx) {
        Map<String, String> values = new HashMap<>();
        for (String field : new String[]{"firstName", "lastName", "email", "phone"}) {
            String value = ctx.formParam(field);
            values.put(field, value != null ? value : "");
        }

        // Robot : on fait comme si la demande était partie, sans rien enregistrer ni envoyer.
        String honeypot = ctx.formParam(HONEYPOT);
        if (honeypot != null && !honeypot.isBlank()) {
            renderRequested(ctx, values.get("firstName"), values.get("email"));
            return;
        }
        if (!requestRateLimiter.tryAcquire(RequestRateLimiter.clientOf(ctx))) {
            getWorkshopSessionHandler.render(ctx, "Trop de demandes depuis votre connexion. Réessayez dans une heure, "
                    + "ou écrivez-nous sur Instagram (@eosa.bijoux).", values);
            return;
        }

        WorkshopBooking workshopBooking;
        try {
            workshopBooking = requestWorkshopBooking.handle(new RequestWorkshopBooking.Command(
                    new WorkshopSessionId(ctx.pathParam("id")),
                    values.get("firstName"),
                    values.get("lastName"),
                    values.get("email"),
                    values.get("phone"),
                    ctx.formParam("acceptedConditions") != null
            ));
        } catch (RuntimeException e) {
            getWorkshopSessionHandler.render(ctx, e.getMessage(), values);
            return;
        }
        renderRequested(ctx, workshopBooking.getFirstName(), workshopBooking.getEmail());
    }

    private void renderRequested(Context ctx, String firstName, String email) {
        var overview = readWorkshopSession.handle(new ReadWorkshopSession.Command(new WorkshopSessionId(ctx.pathParam("id"))));
        Map<String, Object> model = new HashMap<>();
        model.put("title", "Eosa — Confirmez votre place");
        model.put("description", "Confirmez votre place à l'atelier EOSA grâce à l'e-mail que vous venez de recevoir.");
        model.put("ogImageUrl", baseUrl + "/workshops.webp");
        model.put("canonicalUrl", baseUrl + "/workshops/sessions/" + overview.session().getId().value());
        model.put("noindex", true);
        model.put("sessionId", overview.session().getId().value());
        model.put("firstName", firstName);
        model.put("email", email);
        model.put("venueName", overview.venue().getName());
        model.put("date", WorkshopDates.date(overview.session().getStartsAt()));
        model.put("timeRange", WorkshopDates.timeRange(overview.session()));
        model.put("confirmationDelayMinutes", WorkshopBooking.CONFIRMATION_DELAY.toMinutes());
        ctx.render("workshop-booking-requested.mustache", model);
    }

}
