package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadWorkshopSession;
import me.noynto.eosa.application.ReadWorkshopSessions;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopDates;
import me.noynto.eosa.workshop.WorkshopSessionState;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Carte d'une session sur la page des ateliers, le lieu en premier. Elle se recharge toutes les 30 s :
 * le compte à rebours avance, la carte s'ouvre à l'heure d'ouverture et le compteur de places suit
 * les réservations.
 */
public record GetWorkshopSessionCardHandler(
        ReadWorkshopSession readWorkshopSession,
        Clock clock
) implements Handler {

    @Override
    public void handle(Context ctx) {
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
        // Une session annulée ou commencée depuis l'affichage de la page disparaît de la liste.
        if (session.getState() != WorkshopSessionState.SCHEDULED || !now.isBefore(session.getStartsAt())) {
            ctx.html("");
            return;
        }
        boolean notOpenYet = now.isBefore(session.getBookingOpensAt());
        int remainingPlaces = Math.max(0, session.getCapacity() - overview.occupiedPlaces());
        boolean full = !notOpenYet && remainingPlaces == 0;
        boolean open = !notOpenYet && remainingPlaces > 0;
        boolean hasVenueImage = venue.getImageId() != null;

        Map<String, Object> model = new HashMap<>();
        model.put("sessionId", session.getId().value());
        model.put("venueName", venue.getName());
        model.put("venueCity", venue.getCity());
        model.put("hasVenueImage", hasVenueImage);
        model.put("venueImageId", hasVenueImage ? venue.getImageId().value() : "");
        model.put("date", WorkshopDates.date(session.getStartsAt()));
        model.put("timeRange", WorkshopDates.timeRange(session));
        model.put("notOpenYet", notOpenYet);
        model.put("countdown", countdown(Duration.between(now, session.getBookingOpensAt())));
        model.put("bookingOpensAt", WorkshopDates.dateInSentence(session.getBookingOpensAt()) + " à " + WorkshopDates.time(session.getBookingOpensAt()));
        model.put("open", open);
        model.put("full", full);
        model.put("lastPlace", open && remainingPlaces == 1);
        model.put("remainingPlaces", remainingPlaces + " places restantes");
        ctx.render("partials/workshop-session-card.mustache", model);
    }

    /** « dans 3 j 4 h », « dans 5 h 20 min », « dans 12 min », « dans moins d'une minute » */
    private static String countdown(Duration remaining) {
        long minutes = (long) Math.ceil(remaining.toSeconds() / 60.0);
        long days = minutes / (24 * 60);
        long hours = (minutes % (24 * 60)) / 60;
        long rest = minutes % 60;
        if (days > 0) {
            return "dans " + days + " j " + hours + " h";
        }
        if (hours > 0) {
            return "dans " + hours + " h " + rest + " min";
        }
        return minutes > 0 ? "dans " + minutes + " min" : "dans moins d'une minute";
    }

}
