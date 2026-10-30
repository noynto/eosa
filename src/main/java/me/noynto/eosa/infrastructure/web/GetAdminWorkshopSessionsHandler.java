package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadWorkshopSessions;
import me.noynto.eosa.application.ReadWorkshopVenues;
import me.noynto.eosa.workshop.WorkshopSessionState;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record GetAdminWorkshopSessionsHandler(
        ReadWorkshopSessions readWorkshopSessions,
        ReadWorkshopVenues readWorkshopVenues,
        Clock clock
) implements Handler {

    @Override
    public void handle(Context ctx) {
        Instant now = clock.instant();
        var overviews = readWorkshopSessions.handle();

        // À venir : par date croissante ; passées : la plus récente d'abord.
        List<Map<String, Object>> upcoming = overviews.stream()
                .filter(overview -> overview.session().getEndsAt().isAfter(now))
                .map(overview -> rowOf(overview, now))
                .toList();
        List<Map<String, Object>> past = overviews.reversed().stream()
                .filter(overview -> !overview.session().getEndsAt().isAfter(now))
                .map(overview -> rowOf(overview, now))
                .toList();

        var venues = readWorkshopVenues.handle().stream()
                .filter(venue -> !venue.isArchived())
                .map(venue -> {
                    Map<String, Object> option = new HashMap<>();
                    option.put("id", venue.getId().value());
                    option.put("name", venue.getName());
                    option.put("city", venue.getCity());
                    option.put("defaultCapacity", venue.getDefaultCapacity());
                    option.put("defaultDurationMinutes", venue.getDefaultDurationMinutes());
                    return option;
                })
                .toList();

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Eosa — Sessions d'atelier");
        model.put("upcomingSessions", upcoming);
        model.put("hasUpcomingSessions", !upcoming.isEmpty());
        model.put("pastSessions", past);
        model.put("hasPastSessions", !past.isEmpty());
        model.put("venues", venues);
        model.put("hasVenues", !venues.isEmpty());
        model.put("firstVenueCapacity", venues.isEmpty() ? "" : venues.getFirst().get("defaultCapacity"));
        model.put("firstVenueDuration", venues.isEmpty() ? "" : venues.getFirst().get("defaultDurationMinutes"));
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
        row.put("status", WorkshopSessionFormat.status(overview, now));
        row.put("cancelled", session.getState() == WorkshopSessionState.CANCELLED);
        return row;
    }

}
