package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadJewelIds;
import me.noynto.eosa.jewel.JewelCategory;
import me.noynto.eosa.jewel.JewelState;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public record GetJewelsHandler(
        ReadJewelIds readJewelIds,
        Set<JewelCategory> categories,
        String title,
        String baseUrl
) implements Handler {

    @Override
    public void handle(Context ctx) {
        var jewelIds = readJewelIds.handle(new ReadJewelIds.Query(Set.of(JewelState.PUBLISHED), categories));
        Map<String, Object> model = new HashMap<>();
        model.put("title", "Eosa — " + title);
        model.put("heading", title);
        model.put("description", title + " — bijoux faits main à Nancy, en matières nobles, conçus pour durer.");
        model.put("ogImageUrl", baseUrl + "/hero.webp");
        model.put("canonicalUrl", baseUrl + ctx.path());
        model.put("countLabel", jewelIds.size() + " pièce" + (jewelIds.size() > 1 ? "s" : ""));
        model.put("jewelIds", jewelIds.stream().map(id -> Map.of("id", id.value())).toList());
        ctx.render("jewels.mustache", model);
    }

}