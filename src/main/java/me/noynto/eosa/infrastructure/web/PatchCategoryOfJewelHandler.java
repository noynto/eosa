package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.UpdateCategoryOfJewel;
import me.noynto.eosa.jewel.JewelCategory;
import me.noynto.eosa.shared.JewelId;

public record PatchCategoryOfJewelHandler(UpdateCategoryOfJewel updateCategoryOfJewel) implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        JewelCategory category;
        try {
            category = JewelCategory.valueOf(ctx.formParam("category"));
        } catch (IllegalArgumentException e) {
            category = null;
        }
        try {
            updateCategoryOfJewel.handle(new UpdateCategoryOfJewel.Command(
                    new JewelId(ctx.pathParam("jewel-id")),
                    category
            ));
            ctx.html("<span class=\"text-success text-xs\">Sauvegardé</span>");
        } catch (RuntimeException e) {
            ctx.status(422).html("<span class=\"text-red-600 text-xs\">" + e.getMessage() + "</span>");
        }
    }

}