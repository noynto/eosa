package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.CreateCharm;

import java.math.BigDecimal;

public record CreateCharmHandler(CreateCharm createCharm) implements Handler {

    @Override
    public void handle(Context ctx) {
        BigDecimal price;
        try {
            price = new BigDecimal(ctx.formParam("price"));
        } catch (NumberFormatException | NullPointerException e) {
            ctx.status(422).html("<span class=\"text-red-600 text-xs\">Le prix de la breloque est invalide.</span>");
            return;
        }
        createCharm.handle(new CreateCharm.Command(ctx.formParam("name"), price));
        ctx.header("HX-Redirect", "/admin/charms");
        ctx.status(200);
    }

}
