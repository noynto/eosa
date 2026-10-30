package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadWorkshopSession;
import me.noynto.eosa.application.ReadWorkshopSessions;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopDates;
import me.noynto.eosa.workshop.WorkshopSessionState;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Page publique d'une session : le lieu mis en avant, puis le formulaire de réservation quand c'est possible.
 */
public record GetWorkshopSessionHandler(
        ReadWorkshopSession readWorkshopSession,
        Clock clock,
        String baseUrl
) implements Handler {

    @Override
    public void handle(Context ctx) {
        render(ctx, null, null, Map.of());
    }

    /**
     * Affiche la page, avec un message d'erreur et les valeurs saisies si un formulaire a été refusé,
     * ou un message de confirmation (inscription en liste d'attente).
     */
    public void render(Context ctx, String error, String notice, Map<String, String> values) {
        ReadWorkshopSessions.Overview overview;
        try {
            overview = readWorkshopSession.handle(new ReadWorkshopSession.Command(new WorkshopSessionId(ctx.pathParam("id"))));
        } catch (RuntimeException e) {
            ctx.status(404);
            return;
        }
        var session = overview.session();
        var venue = overview.venue();
        Instant now = clock.instant();
        boolean cancelled = session.getState() == WorkshopSessionState.CANCELLED;
        boolean started = !now.isBefore(session.getStartsAt());
        boolean notOpenYet = !cancelled && !started && now.isBefore(session.getBookingOpensAt());
        int remainingPlaces = Math.max(0, session.getCapacity() - overview.occupiedPlaces());
        boolean full = !cancelled && !started && !notOpenYet && remainingPlaces == 0;
        boolean open = !cancelled && !started && !notOpenYet && remainingPlaces > 0;
        String venueAddress = venue.getAddress() + ", " + venue.getPostalCode() + " " + venue.getCity();
        boolean hasVenueImage = venue.getImageId() != null;

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Eosa — Atelier chez " + venue.getName() + ", " + WorkshopDates.dateInSentence(session.getStartsAt()));
        model.put("description", "Créez votre collier ou bracelet en pierres naturelles lors d'un atelier EOSA chez "
                + venue.getName() + " (" + venue.getCity() + "), le " + WorkshopDates.dateInSentence(session.getStartsAt()) + ".");
        model.put("ogImageUrl", hasVenueImage ? baseUrl + "/images/" + venue.getImageId().value() : baseUrl + "/workshops.webp");
        model.put("canonicalUrl", baseUrl + "/workshops/sessions/" + session.getId().value());
        model.put("noindex", true);
        model.put("sessionId", session.getId().value());
        model.put("venueName", venue.getName());
        model.put("hasVenueDescription", venue.getDescription() != null);
        model.put("venueDescription", venue.getDescription() != null ? venue.getDescription() : "");
        model.put("venueAddress", venueAddress);
        model.put("mapUrl", "https://www.google.com/maps/search/?api=1&query=" + URLEncoder.encode(venueAddress, StandardCharsets.UTF_8));
        model.put("hasVenueLink", venue.getLinkUrl() != null);
        model.put("venueLinkUrl", venue.getLinkUrl() != null ? venue.getLinkUrl() : "");
        model.put("hasVenueImage", hasVenueImage);
        model.put("venueImageId", hasVenueImage ? venue.getImageId().value() : "");
        model.put("date", WorkshopDates.date(session.getStartsAt()));
        model.put("timeRange", WorkshopDates.timeRange(session));
        model.put("duration", WorkshopDates.duration(session.getDurationMinutes()));
        model.put("cancelled", cancelled);
        model.put("started", started && !cancelled);
        model.put("notOpenYet", notOpenYet);
        model.put("bookingOpensAt", WorkshopDates.dateInSentence(session.getBookingOpensAt()) + " à " + WorkshopDates.time(session.getBookingOpensAt()));
        model.put("full", full);
        model.put("open", open);
        model.put("remainingPlaces", remainingPlaces == 1 ? "Dernière place" : remainingPlaces + " places restantes");
        model.put("hasError", error != null);
        model.put("error", error != null ? error : "");
        model.put("hasNotice", notice != null);
        model.put("notice", notice != null ? notice : "");
        model.put("firstName", values.getOrDefault("firstName", ""));
        model.put("lastName", values.getOrDefault("lastName", ""));
        model.put("email", values.getOrDefault("email", ""));
        model.put("phone", values.getOrDefault("phone", ""));
        if (error != null) {
            ctx.status(422);
        }
        ctx.render("workshop-session.mustache", model);
    }

}
