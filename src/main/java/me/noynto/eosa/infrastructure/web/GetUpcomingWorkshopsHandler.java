package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadUpcomingWorkshopSessionIds;

import java.util.Map;

/**
 * Fragment de la page d'accueil : la section des prochains ateliers, dont chaque carte se charge ensuite d'elle-même.
 * Sans session à venir, le fragment est vide et la section n'apparaît pas.
 */
public record GetUpcomingWorkshopsHandler(ReadUpcomingWorkshopSessionIds readUpcomingWorkshopSessionIds) implements Handler {

    private static final int COUNT = 3;

    @Override
    public void handle(Context ctx) {
        var ids = readUpcomingWorkshopSessionIds.handle(new ReadUpcomingWorkshopSessionIds.Query(COUNT)).workshopSessionIds();
        ctx.render("partials/upcoming-workshops.mustache", Map.of(
                "hasUpcomingSessions", !ids.isEmpty(),
                "upcomingSessions", ids.stream().map(id -> Map.of("id", id.value())).toList()
        ));
    }

}
