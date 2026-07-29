package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadJewel;
import me.noynto.eosa.jewel.JewelState;
import me.noynto.eosa.shared.JewelId;

import java.util.HashMap;
import java.util.Map;

public record GetAdminJewelRowHandler(ReadJewel readJewel) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            var jewel = readJewel.handle(new ReadJewel.Command(new JewelId(ctx.pathParam("id"))));
            boolean hasState = jewel.getState() != null;
            boolean hasCategory = jewel.getCategory() != null;
            Map<String, Object> model = new HashMap<>();
            model.put("jewelId", jewel.getId().value());
            model.put("name", jewel.getName());
            model.put("hasState", hasState);
            model.put("stateLabel", hasState ? jewel.getState().name().toLowerCase() : "");
            model.put("stateBadgeClass", hasState ? stateBadgeClass(jewel.getState()) : "");
            model.put("hasCategory", hasCategory);
            model.put("categoryLabel", hasCategory ? jewel.getCategory().name().toLowerCase() : "");
            ctx.render("admin/partials/jewel-row.mustache", model);
        } catch (RuntimeException e) {
            ctx.status(404);
        }
    }

    private static String stateBadgeClass(JewelState state) {
        return switch (state) {
            case PUBLISHED -> "bg-success/10 text-success";
            case DRAFTED -> "bg-bg-border text-secondary";
            default -> "bg-bg-border text-muted";
        };
    }

}