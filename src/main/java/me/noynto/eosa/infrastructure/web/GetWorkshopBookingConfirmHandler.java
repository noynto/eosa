package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadWorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingState;
import me.noynto.eosa.workshop.WorkshopDates;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Page du lien « Confirmer ma place ». Un simple affichage ne confirme rien (les messageries ouvrent
 * parfois les liens d'elles-mêmes) : la confirmation se fait avec le bouton, en POST.
 */
public record GetWorkshopBookingConfirmHandler(
        ReadWorkshopBooking readWorkshopBooking,
        Clock clock,
        String baseUrl
) implements Handler {

    private static final Duration CANCELLATION_DEADLINE = Duration.ofHours(48);

    @Override
    public void handle(Context ctx) {
        ReadWorkshopBooking.Overview overview;
        try {
            overview = readWorkshopBooking.handle(new ReadWorkshopBooking.Command(ctx.pathParam("token")));
        } catch (RuntimeException e) {
            ctx.status(404);
            return;
        }
        var booking = overview.booking();
        var session = overview.session();
        var venue = overview.venue();
        Instant now = clock.instant();
        boolean awaiting = booking.isAwaitingConfirmation(now);
        boolean confirmed = booking.getState() == WorkshopBookingState.CONFIRMED
                || booking.getState() == WorkshopBookingState.ATTENDED
                || booking.getState() == WorkshopBookingState.NO_SHOW;
        boolean cancelled = booking.getState() == WorkshopBookingState.CANCELLED;
        boolean expired = !awaiting && !confirmed && !cancelled;
        String venueAddress = venue.getAddress() + ", " + venue.getPostalCode() + " " + venue.getCity();
        boolean hasVenueImage = venue.getImageId() != null;
        Instant cancellationDeadline = session.getStartsAt().minus(CANCELLATION_DEADLINE);

        Map<String, Object> model = new HashMap<>();
        model.put("title", confirmed ? "Eosa — Votre place est réservée" : "Eosa — Confirmez votre place");
        model.put("description", "Votre réservation à l'atelier EOSA chez " + venue.getName() + ".");
        model.put("ogImageUrl", baseUrl + "/workshops.webp");
        model.put("canonicalUrl", baseUrl + "/workshops");
        model.put("noindex", true);
        model.put("token", booking.getToken());
        model.put("sessionId", session.getId().value());
        model.put("firstName", booking.getFirstName());
        model.put("email", booking.getEmail());
        model.put("venueName", venue.getName());
        model.put("venueAddress", venueAddress);
        model.put("mapUrl", "https://www.google.com/maps/search/?api=1&query=" + URLEncoder.encode(venueAddress, StandardCharsets.UTF_8));
        model.put("hasVenueImage", hasVenueImage);
        model.put("venueImageId", hasVenueImage ? venue.getImageId().value() : "");
        model.put("date", WorkshopDates.date(session.getStartsAt()));
        model.put("timeRange", WorkshopDates.timeRange(session));
        model.put("duration", WorkshopDates.duration(session.getDurationMinutes()));
        model.put("awaiting", awaiting);
        model.put("confirmed", confirmed);
        model.put("cancelled", cancelled);
        model.put("expired", expired);
        model.put("canStillCancel", confirmed && now.isBefore(cancellationDeadline));
        model.put("cancellationDeadline", WorkshopDates.dateInSentence(cancellationDeadline) + " à " + WorkshopDates.time(cancellationDeadline));
        ctx.render("workshop-booking-confirm.mustache", model);
    }

}
