package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadJewel;
import me.noynto.eosa.jewel.JewelCategory;
import me.noynto.eosa.jewel.JewelState;
import me.noynto.eosa.shared.JewelId;

import java.util.HashMap;
import java.util.Map;

public record GetAdminJewelHandler(ReadJewel readJewel) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            var jewel = readJewel.handle(new ReadJewel.Command(new JewelId(ctx.pathParam("id"))));
            Map<String, Object> model = new HashMap<>();
            model.put("title", jewel.getName());
            model.put("name", jewel.getName());
            model.put("jewelId", jewel.getId().value());
            model.put("tagline", jewel.getTagline() != null ? jewel.getTagline() : "");
            model.put("price", jewel.getPrice() != null ? jewel.getPrice().toPlainString() : "");
            model.put("categoryNecklaceSelected", jewel.getCategory() == JewelCategory.NECKLACE);
            model.put("categoryBraceletSelected", jewel.getCategory() == JewelCategory.BRACELET);
            model.put("stateDraftedSelected", jewel.getState() == JewelState.DRAFTED);
            model.put("statePublishedSelected", jewel.getState() == JewelState.PUBLISHED);
            model.put("stateArchivedSelected", jewel.getState() == JewelState.ARCHIVED);
            model.put("hasImages", !jewel.getImageIds().isEmpty());
            model.put("images", jewel.getImageIds().stream().map(id -> Map.of("id", id.value())).toList());
            ctx.render("admin/jewel.mustache", model);
        } catch (RuntimeException e) {
            ctx.status(404);
        }
    }

}