package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadJewelIds;
import me.noynto.eosa.jewel.JewelState;

import java.util.Map;
import java.util.Set;

/**
 * Fragment de la page d'accueil : l'emplacement de chacun des derniers bijoux publiés, qui se charge ensuite
 * lui-même par sa carte.
 */
public record GetLatestJewelsHandler(ReadJewelIds readJewelIds) implements Handler {

    private static final int COUNT = 4;

    @Override
    public void handle(Context ctx) {
        var jewelIds = readJewelIds.handle(new ReadJewelIds.Query(Set.of(JewelState.PUBLISHED), Set.of(), COUNT));
        ctx.render("partials/latest-jewels.mustache", Map.of(
                "latestJewelIds", jewelIds.stream().map(id -> Map.of("id", id.value())).toList()
        ));
    }

}
