package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadCharms;

import java.util.HashMap;
import java.util.Map;

public record GetAdminCharmsHandler(ReadCharms readCharms) implements Handler {

    @Override
    public void handle(Context ctx) {
        var charms = readCharms.handle();

        var rows = charms.stream().map(charm -> {
            boolean hasImage = charm.getImageId() != null;
            Map<String, Object> row = new HashMap<>();
            row.put("id", charm.getId().value());
            row.put("name", charm.getName());
            row.put("price", charm.getPrice().stripTrailingZeros().toPlainString());
            row.put("hasImage", hasImage);
            row.put("imageId", hasImage ? charm.getImageId().value() : "");
            return row;
        }).toList();

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Eosa — Breloques");
        model.put("charms", rows);
        model.put("hasCharms", !rows.isEmpty());
        ctx.render("admin/charms.mustache", model);
    }

}
