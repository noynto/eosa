package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.UpdateCharm;
import me.noynto.eosa.shared.CharmId;

import java.math.BigDecimal;

public record UpdateCharmHandler(UpdateCharm updateCharm) implements Handler {

    @Override
    public void handle(Context ctx) {
        BigDecimal price;
        try {
            price = new BigDecimal(ctx.formParam("price"));
        } catch (NumberFormatException | NullPointerException e) {
            price = null;
        }
        try {
            updateCharm.handle(new UpdateCharm.Command(
                    new CharmId(ctx.pathParam("id")),
                    ctx.formParam("name"),
                    price
            ));
            ctx.html("<span class=\"text-success text-xs\">Sauvegardé</span>");
        } catch (RuntimeException e) {
            ctx.status(422).html("<span class=\"text-red-600 text-xs\">" + e.getMessage() + "</span>");
        }
    }

}
