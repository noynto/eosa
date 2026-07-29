package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadJewel;
import me.noynto.eosa.shared.JewelId;

import java.util.HashMap;
import java.util.Map;

public record GetJewelCardHandler(ReadJewel readJewel) implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        try {
            var jewel = readJewel.handle(new ReadJewel.Command(new JewelId(ctx.pathParam("id"))));
            boolean hasImage = !jewel.getImageIds().isEmpty();
            boolean hasTagline = jewel.getTagline() != null;
            Map<String, Object> model = new HashMap<>();
            model.put("jewelId", jewel.getId().value());
            model.put("hasImage", hasImage);
            model.put("mainImageId", hasImage ? jewel.getImageIds().getFirst().value() : "");
            model.put("name", jewel.getName());
            model.put("price", jewel.getPrice().stripTrailingZeros().toPlainString());
            model.put("hasTagline", hasTagline);
            model.put("tagline", hasTagline ? jewel.getTagline() : "");
            ctx.render("partials/jewel-card.mustache", model);
        } catch (RuntimeException e) {
            ctx.status(404);
        }
    }

}