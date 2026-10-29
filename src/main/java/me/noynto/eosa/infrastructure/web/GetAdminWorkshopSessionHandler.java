package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadWorkshopSession;
import me.noynto.eosa.application.ReadWorkshopSessions;
import me.noynto.eosa.application.ReadWorkshopVenues;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopSessionState;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public record GetAdminWorkshopSessionHandler(
        ReadWorkshopSession readWorkshopSession,
        ReadWorkshopVenues readWorkshopVenues,
        Clock clock
) implements Handler {

    @Override
    public void handle(Context ctx) {
        ReadWorkshopSessions.Overview sessionOverview;
        try {
            sessionOverview = readWorkshopSession.handle(new ReadWorkshopSession.Command(new WorkshopSessionId(ctx.pathParam("id"))));
        } catch (RuntimeException e) {
            ctx.status(404);
            return;
        }
        Instant now = clock.instant();
        var session = sessionOverview.session();
        var currentVenue = sessionOverview.venue();
        boolean cancelled = session.getState() == WorkshopSessionState.CANCELLED;
        boolean editable = !cancelled && session.getStartsAt().isAfter(now);

        // Les lieux actifs, plus le lieu actuel même s'il a été archivé depuis.
        var venues = readWorkshopVenues.handle().stream()
                .filter(venue -> !venue.isArchived() || venue.getId().equals(currentVenue.getId()))
                .map(venue -> {
                    Map<String, Object> option = new HashMap<>();
                    option.put("id", venue.getId().value());
                    option.put("name", venue.getName());
                    option.put("city", venue.getCity());
                    option.put("selected", venue.getId().equals(currentVenue.getId()));
                    return option;
                })
                .toList();

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Session du " + WorkshopSessionFormat.date(session.getStartsAt()));
        model.put("workshopSessionId", session.getId().value());
        model.put("date", WorkshopSessionFormat.date(session.getStartsAt()));
        model.put("timeRange", WorkshopSessionFormat.timeRange(session));
        model.put("venueId", currentVenue.getId().value());
        model.put("venueName", currentVenue.getName());
        model.put("venueAddress", currentVenue.getAddress() + ", " + currentVenue.getPostalCode() + " " + currentVenue.getCity());
        model.put("dateInput", WorkshopSessionFormat.dateInput(session.getStartsAt()));
        model.put("timeInput", WorkshopSessionFormat.timeInput(session.getStartsAt()));
        model.put("durationMinutes", session.getDurationMinutes());
        model.put("capacity", session.getCapacity());
        model.put("occupiedPlaces", sessionOverview.occupiedPlaces());
        model.put("hasBookings", sessionOverview.occupiedPlaces() > 0);
        model.put("bookingOpensAt", WorkshopSessionFormat.date(session.getBookingOpensAt())
                + " à " + WorkshopSessionFormat.time(session.getBookingOpensAt()));
        model.put("status", WorkshopSessionFormat.status(sessionOverview, now));
        model.put("cancelled", cancelled);
        model.put("editable", editable);
        model.put("venues", venues);
        ctx.render("admin/workshop.mustache", model);
    }

}
