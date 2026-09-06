package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadMetalColors;

import java.util.HashMap;
import java.util.Map;

public record GetAdminMetalColorsHandler(ReadMetalColors readMetalColors) implements Handler {

    @Override
    public void handle(Context ctx) {
        var metalColors = readMetalColors.handle();

        var rows = metalColors.stream().map(metalColor -> {
            boolean hasImage = metalColor.getImageId() != null;
            Map<String, Object> row = new HashMap<>();
            row.put("id", metalColor.getId().value());
            row.put("name", metalColor.getName());
            row.put("hasImage", hasImage);
            row.put("imageId", hasImage ? metalColor.getImageId().value() : "");
            return row;
        }).toList();

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Eosa — Couleurs de métal");
        model.put("metalColors", rows);
        model.put("hasMetalColors", !rows.isEmpty());
        ctx.render("admin/metal-colors.mustache", model);
    }

}
