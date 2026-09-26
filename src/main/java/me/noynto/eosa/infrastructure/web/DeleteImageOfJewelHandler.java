package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.RemoveImageFromJewel;
import me.noynto.eosa.jewel.Jewel;
import me.noynto.eosa.shared.IdentityId;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.JewelId;

import java.util.HashMap;
import java.util.Map;

public record DeleteImageOfJewelHandler(RemoveImageFromJewel removeImageFromJewel) implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        try {
            IdentityId identityId = ctx.attribute("identityId");
            Jewel jewel = removeImageFromJewel.handle(new RemoveImageFromJewel.Command(
                    identityId,
                    new JewelId(ctx.pathParam("jewel-id")),
                    new ImageId(ctx.pathParam("image-id"))
            ));
            Map<String, Object> model = new HashMap<>();
            model.put("jewelId", jewel.getId().value());
            model.put("hasImages", !jewel.getImageIds().isEmpty());
            model.put("images", jewel.getImageIds().stream().map(id -> Map.of("id", id.value())).toList());
            ctx.render("admin/partials/jewel-images.mustache", model);
        } catch (RuntimeException e) {
            ctx.status(422).html("<span class=\"text-red-600 text-xs\">" + e.getMessage() + "</span>");
        }
    }

}
