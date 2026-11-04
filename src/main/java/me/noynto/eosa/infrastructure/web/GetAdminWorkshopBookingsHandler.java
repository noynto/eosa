package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadWorkshopBookings;
import me.noynto.eosa.shared.WorkshopSessionId;
import me.noynto.eosa.workshop.WorkshopBooking;
import me.noynto.eosa.workshop.WorkshopBookingState;
import me.noynto.eosa.workshop.WorkshopDates;
import me.noynto.eosa.workshop.WorkshopSession;
import me.noynto.eosa.workshop.WorkshopSessionState;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Fragment de la page d'une session pour l'administration : la section « Inscrites », avec ce qu'on peut faire de
 * chaque réservation (annuler, pointer présente ou absente).
 */
public record GetAdminWorkshopBookingsHandler(
        ReadWorkshopBookings readWorkshopBookings,
        Clock clock
) implements Handler {

    private static final DateTimeFormatter BOOKED_AT = DateTimeFormatter.ofPattern("d MMM à H 'h' mm", Locale.FRENCH);

    @Override
    public void handle(Context ctx) {
        ReadWorkshopBookings.Result result;
        try {
            result = readWorkshopBookings.handle(new ReadWorkshopBookings.Command(new WorkshopSessionId(ctx.pathParam("id"))));
        } catch (RuntimeException e) {
            ctx.status(404);
            return;
        }
        Instant now = clock.instant();
        var session = result.session();
        boolean cancelled = session.getState() == WorkshopSessionState.CANCELLED;

        // Après le début : pointage présente / absente (absente seulement après la tolérance de retard).
        boolean started = !cancelled && !now.isBefore(session.getStartsAt());
        Instant noShowFrom = session.getStartsAt().plus(WorkshopBooking.NO_SHOW_GRACE_PERIOD);
        boolean noShowAvailable = !now.isBefore(noShowFrom);

        Map<String, Object> model = new HashMap<>();
        model.put("workshopSessionId", session.getId().value());
        model.put("started", started);
        model.put("noShowFrom", WorkshopDates.time(noShowFrom));
        model.put("hasBookingsToMark", started && result.bookings().stream()
                .anyMatch(booking -> booking.getState() == WorkshopBookingState.CONFIRMED));
        var rows = result.bookings().stream()
                .map(booking -> rowOf(booking, session, cancelled, started, noShowAvailable, now))
                .toList();
        model.put("bookings", rows);
        model.put("hasBookingRows", !rows.isEmpty());
        ctx.render("admin/partials/workshop-bookings.mustache", model);
    }

    private static Map<String, Object> rowOf(WorkshopBooking booking, WorkshopSession session, boolean cancelled,
                                             boolean started, boolean noShowAvailable, Instant now) {
        boolean markable = started && (booking.getState() == WorkshopBookingState.CONFIRMED
                || booking.getState() == WorkshopBookingState.ATTENDED
                || booking.getState() == WorkshopBookingState.NO_SHOW);
        Map<String, Object> row = new HashMap<>();
        row.put("id", booking.getId().value());
        row.put("canCancel", !cancelled && booking.isActive(now) && now.isBefore(session.getStartsAt()));
        row.put("canMarkAttended", markable && booking.getState() != WorkshopBookingState.ATTENDED);
        row.put("canMarkNoShow", markable && booking.getState() != WorkshopBookingState.NO_SHOW);
        row.put("noShowAvailable", noShowAvailable);
        row.put("name", booking.getFirstName() + " " + booking.getLastName());
        row.put("email", booking.getEmail());
        row.put("hasPhone", booking.getPhone() != null);
        row.put("phone", booking.getPhone() != null ? booking.getPhone() : "");
        row.put("bookedAt", BOOKED_AT.format(booking.getCreatedAt().atZone(WorkshopSession.ZONE)));
        row.put("state", switch (booking.getState()) {
            case PENDING -> booking.isAwaitingConfirmation(now) ? "En attente de confirmation" : "Non confirmée";
            case CONFIRMED -> "Confirmée";
            case CANCELLED -> "Annulée";
            case EXPIRED -> "Non confirmée";
            case ATTENDED -> "Présente";
            case NO_SHOW -> "Absente";
        });
        return row;
    }

}
