package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadWorkshopSessions;
import me.noynto.eosa.workshop.WorkshopSessionState;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Liste des sessions pour l'administration. Le formulaire de création, qui a besoin des lieux, se charge à la
 * demande (voir {@link GetAdminWorkshopFormHandler}).
 */
public record GetAdminWorkshopSessionsHandler(
        ReadWorkshopSessions readWorkshopSessions,
        Clock clock
) implements Handler {

    @Override
    public void handle(Context ctx) {
        Instant now = clock.instant();
        var overviews = readWorkshopSessions.handle().overviews();

        // À venir : par date croissante ; passées : la plus récente d'abord.
        List<Map<String, Object>> upcoming = overviews.stream()
                .filter(overview -> overview.session().getEndsAt().isAfter(now))
                .map(overview -> rowOf(overview, now))
                .toList();
        List<Map<String, Object>> past = overviews.reversed().stream()
                .filter(overview -> !overview.session().getEndsAt().isAfter(now))
                .map(overview -> rowOf(overview, now))
                .toList();

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Eosa — Sessions d'atelier");
        model.put("upcomingSessions", upcoming);
        model.put("hasUpcomingSessions", !upcoming.isEmpty());
        model.put("pastSessions", past);
        model.put("hasPastSessions", !past.isEmpty());
        ctx.render("admin/workshops.mustache", model);
    }

    private static Map<String, Object> rowOf(ReadWorkshopSessions.Overview overview, Instant now) {
        var session = overview.session();
        Map<String, Object> row = new HashMap<>();
        row.put("id", session.getId().value());
        row.put("date", WorkshopSessionFormat.date(session.getStartsAt()));
        row.put("timeRange", WorkshopSessionFormat.timeRange(session));
        row.put("venueName", overview.venue().getName());
        row.put("venueCity", overview.venue().getCity());
        row.put("occupiedPlaces", overview.occupiedPlaces());
        row.put("capacity", session.getCapacity());
        row.put("hasWaitlist", overview.waitlistCount() > 0);
        row.put("waitlistCount", overview.waitlistCount());
        row.put("status", WorkshopSessionFormat.status(session, overview.occupiedPlaces(), now));
        row.put("cancelled", session.getState() == WorkshopSessionState.CANCELLED);
        return row;
    }

}
