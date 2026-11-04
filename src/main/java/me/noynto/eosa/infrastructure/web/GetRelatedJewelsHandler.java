package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadRelatedJewels;
import me.noynto.eosa.shared.JewelId;

import java.util.HashMap;
import java.util.Map;

/**
 * Fragment de la fiche d'un bijou : la section « D'autres bijoux », vide s'il n'y en a pas. Chaque bijou se charge
 * ensuite par sa carte.
 */
public record GetRelatedJewelsHandler(ReadRelatedJewels readRelatedJewels) implements Handler {

    private static final int COUNT = 4;

    @Override
    public void handle(Context ctx) {
        var jewelIds = readRelatedJewels.handle(new ReadRelatedJewels.Query(new JewelId(ctx.pathParam("id")), COUNT)).jewelIds();
        Map<String, Object> model = new HashMap<>();
        model.put("hasRelated", !jewelIds.isEmpty());
        model.put("relatedIds", jewelIds.stream().map(id -> Map.of("id", id.value())).toList());
        ctx.render("partials/related-jewels.mustache", model);
    }

}
