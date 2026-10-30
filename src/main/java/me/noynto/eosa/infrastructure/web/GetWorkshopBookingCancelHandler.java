package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadWorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingState;
import me.noynto.eosa.workshop.WorkshopDates;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Page du lien « annuler votre réservation » de l'e-mail récapitulatif. Comme pour la confirmation,
 * un simple affichage n'annule rien : l'annulation se fait avec le bouton, en POST.
 */
public record GetWorkshopBookingCancelHandler(
        ReadWorkshopBooking readWorkshopBooking,
        Clock clock,
        String baseUrl
) implements Handler {

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
        Instant cancellationDeadline = session.getStartsAt().minus(WorkshopBooking.SELF_CANCELLATION_DEADLINE);
        boolean cancelled = booking.getState() == WorkshopBookingState.CANCELLED;
        boolean active = booking.isActive(now) && now.isBefore(session.getStartsAt());
        boolean cancellable = active && now.isBefore(cancellationDeadline);
        boolean tooLate = active && !cancellable;
        boolean notActive = !cancelled && !active;

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Eosa — Annuler ma réservation");
        model.put("description", "Annulation de votre réservation à l'atelier EOSA chez " + venue.getName() + ".");
        model.put("ogImageUrl", baseUrl + "/workshops.webp");
        model.put("canonicalUrl", baseUrl + "/workshops");
        model.put("noindex", true);
        model.put("token", booking.getToken());
        model.put("firstName", booking.getFirstName());
        model.put("venueName", venue.getName());
        model.put("date", WorkshopDates.date(session.getStartsAt()));
        model.put("timeRange", WorkshopDates.timeRange(session));
        model.put("cancellationDeadline", WorkshopDates.dateInSentence(cancellationDeadline) + " à " + WorkshopDates.time(cancellationDeadline));
        model.put("cancellable", cancellable);
        model.put("tooLate", tooLate);
        model.put("cancelled", cancelled);
        model.put("notActive", notActive);
        ctx.render("workshop-booking-cancel.mustache", model);
    }

}
