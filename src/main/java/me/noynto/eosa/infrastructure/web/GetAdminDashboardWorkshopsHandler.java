package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadDashboardWorkshops;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Bloc « Prochains ateliers » du tableau de bord. */
public record GetAdminDashboardWorkshopsHandler(
        ReadDashboardWorkshops readDashboardWorkshops,
        Clock clock
) implements Handler {

    @Override
    public void handle(Context ctx) {
        Instant now = clock.instant();
        List<Map<String, Object>> rows = readDashboardWorkshops.handle().overviews().stream().map(overview -> {
            var session = overview.session();
            int capacity = session.getCapacity();
            boolean notOpenYet = now.isBefore(session.getBookingOpensAt());
            boolean full = !notOpenYet && overview.occupiedPlaces() >= capacity;
            Map<String, Object> row = new HashMap<>();
            row.put("id", session.getId().value());
            row.put("date", DashboardFormat.capitalizedDay(session.getStartsAt()));
            row.put("timeRange", WorkshopSessionFormat.timeRange(session));
            row.put("venue", overview.venue().getName() + ", " + overview.venue().getCity());
            row.put("percent", capacity == 0 ? 0 : Math.min(100, overview.occupiedPlaces() * 100 / capacity));
            row.put("ariaLabel", overview.occupiedPlaces() + " places sur " + capacity + " réservées");
            row.put("status", notOpenYet
                    ? "Ouvre le " + DashboardFormat.shortDay(session.getBookingOpensAt())
                    : full ? "Complet" + (overview.waitlistCount() > 0 ? " · " + overview.waitlistCount() + " en attente" : "")
                    : overview.occupiedPlaces() + " / " + capacity + " places");
            return row;
        }).toList();

        Map<String, Object> model = new HashMap<>();
        model.put("sessions", rows);
        model.put("hasSessions", !rows.isEmpty());
        ctx.render("admin/dashboard/workshops.mustache", model);
    }

}
