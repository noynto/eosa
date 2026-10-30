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
 * Page des ateliers : présentation, prochaines sessions (chaque carte se charge puis se rafraîchit
 * d'elle-même), déroulé et tarifs.
 */
public record GetWorkshopsHandler(
        ReadWorkshopSessions readWorkshopSessions,
        Clock clock,
        String baseUrl
) implements Handler {

    @Override
    public void handle(Context ctx) {
        Instant now = clock.instant();
        List<Map<String, Object>> upcomingSessions = readWorkshopSessions.handle().stream()
                .filter(overview -> overview.session().getState() == WorkshopSessionState.SCHEDULED)
                .filter(overview -> overview.session().getStartsAt().isAfter(now))
                .map(overview -> Map.<String, Object>of("id", overview.session().getId().value()))
                .toList();

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Eosa — Ateliers");
        model.put("description", "Créez votre propre collier ou bracelet en pierres naturelles lors d'un atelier EOSA à Nancy, en petit groupe.");
        model.put("ogImageUrl", baseUrl + "/workshops.webp");
        model.put("canonicalUrl", baseUrl + ctx.path());
        model.put("upcomingSessions", upcomingSessions);
        model.put("hasUpcomingSessions", !upcomingSessions.isEmpty());
        ctx.render("workshops.mustache", model);
    }

}
