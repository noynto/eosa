package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadJewelIds;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public record GetAdminJewelsHandler(ReadJewelIds readJewelIds) implements Handler {

    @Override
    public void handle(Context ctx) {
        var ids = readJewelIds.handle(new ReadJewelIds.Query(Set.of(), Set.of()));
        Map<String, Object> model = new HashMap<>();
        model.put("title", "Produits");
        model.put("jewelIds", ids.stream().map(id -> Map.of("id", id.value())).toList());
        ctx.render("admin/jewels.mustache", model);
    }

}