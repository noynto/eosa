package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.UpdateTaglineOfJewel;
import me.noynto.eosa.shared.JewelId;

public record PatchTaglineOfJewelHandler(UpdateTaglineOfJewel updateTaglineOfJewel) implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        try {
            updateTaglineOfJewel.handle(new UpdateTaglineOfJewel.Command(
                    new JewelId(ctx.pathParam("jewel-id")),
                    ctx.formParam("tagline")
            ));
            ctx.html("<span class=\"text-success text-xs\">Sauvegardé</span>");
        } catch (RuntimeException e) {
            ctx.status(422).html("<span class=\"text-red-600 text-xs\">" + e.getMessage() + "</span>");
        }
    }

}