package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.UpdateNameOfJewel;
import me.noynto.eosa.shared.JewelId;

import java.util.Map;

public record PatchNameOfJewelHandler(UpdateNameOfJewel updateNameOfJewel) implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        try {
            var jewel = updateNameOfJewel.handle(new UpdateNameOfJewel.Command(
                    new JewelId(ctx.pathParam("jewel-id")),
                    ctx.formParam("name")
            )).jewel();
            // Le message de la section, et le titre de la page remplacé au passage (hx-swap-oob).
            ctx.render("admin/partials/jewel-renamed.mustache", Map.of("name", jewel.getName()));
        } catch (RuntimeException e) {
            InlineError.render(ctx, e);
        }
    }

}
