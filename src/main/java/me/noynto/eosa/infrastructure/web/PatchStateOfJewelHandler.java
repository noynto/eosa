package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.UpdateStateOfJewel;
import me.noynto.eosa.jewel.JewelState;
import me.noynto.eosa.shared.JewelId;

public record PatchStateOfJewelHandler(UpdateStateOfJewel updateStateOfJewel) implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        JewelState state;
        try {
            state = JewelState.valueOf(ctx.formParam("state"));
        } catch (IllegalArgumentException e) {
            state = null;
        }
        try {
            updateStateOfJewel.handle(new UpdateStateOfJewel.Command(
                    new JewelId(ctx.pathParam("jewel-id")),
                    state
            ));
            ctx.html("<span class=\"text-success text-xs\">Sauvegardé</span>");
        } catch (RuntimeException e) {
            ctx.status(422).html("<span class=\"text-red-600 text-xs\">" + e.getMessage() + "</span>");
        }
    }

}