package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadWorkshopSession;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopSessionState;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Page d'une session pour l'administration : résumé, modification et annulation. Les réservations, la liste
 * d'attente et les lieux proposés se chargent à part (voir {@link GetAdminWorkshopBookingsHandler},
 * {@link GetAdminWorkshopWaitlistHandler} et {@link GetAdminWorkshopVenueOptionsHandler}).
 */
public record GetAdminWorkshopSessionHandler(
        ReadWorkshopSession readWorkshopSession,
        Clock clock
) implements Handler {

    @Override
    public void handle(Context ctx) {
        ReadWorkshopSession.Result sessionResult;
        try {
            sessionResult = readWorkshopSession.handle(new ReadWorkshopSession.Command(new WorkshopSessionId(ctx.pathParam("id"))));
        } catch (RuntimeException e) {
            ctx.status(404);
            return;
        }
        Instant now = clock.instant();
        var session = sessionResult.session();
        var venue = sessionResult.venue();
        boolean cancelled = session.getState() == WorkshopSessionState.CANCELLED;
        boolean editable = !cancelled && session.getStartsAt().isAfter(now);

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Session du " + WorkshopSessionFormat.date(session.getStartsAt()));
        model.put("workshopSessionId", session.getId().value());
        model.put("date", WorkshopSessionFormat.date(session.getStartsAt()));
        model.put("timeRange", WorkshopSessionFormat.timeRange(session));
        model.put("venueId", venue.getId().value());
        model.put("venueName", venue.getName());
        model.put("venueAddress", venue.getAddress() + ", " + venue.getPostalCode() + " " + venue.getCity());
        model.put("dateInput", WorkshopSessionFormat.dateInput(session.getStartsAt()));
        model.put("timeInput", WorkshopSessionFormat.timeInput(session.getStartsAt()));
        model.put("durationMinutes", session.getDurationMinutes());
        model.put("capacity", session.getCapacity());
        model.put("occupiedPlaces", sessionResult.occupiedPlaces());
        model.put("hasBookings", sessionResult.occupiedPlaces() > 0);
        model.put("bookingOpensAt", WorkshopSessionFormat.date(session.getBookingOpensAt())
                + " à " + WorkshopSessionFormat.time(session.getBookingOpensAt()));
        model.put("status", WorkshopSessionFormat.status(session, sessionResult.occupiedPlaces(), now));
        model.put("cancelled", cancelled);
        model.put("editable", editable);
        ctx.render("admin/workshop.mustache", model);
    }

}
