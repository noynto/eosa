package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

/**
 * Page d'accueil de l'administration : la coquille du tableau de bord. Chaque bloc se charge par un appel htmx
 * complémentaire, avec son propre cas d'usage (voir les {@code GetAdminDashboard…Handler}).
 */
public record GetAdminDashboardHandler(Clock clock) implements Handler {

    @Override
    public void handle(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        model.put("title", "Eosa — Tableau de bord");
        model.put("today", DashboardFormat.today(clock.instant()));
        ctx.render("admin/dashboard.mustache", model);
    }

}
