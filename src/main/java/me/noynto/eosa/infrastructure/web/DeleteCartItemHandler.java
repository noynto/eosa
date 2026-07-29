package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import me.noynto.eosa.application.RemoveJewelFromCart;
import me.noynto.eosa.shared.CartId;
import me.noynto.eosa.shared.JewelId;

public record DeleteCartItemHandler(
        RemoveJewelFromCart removeJewelFromCart
) implements Handler {

    @Override
    public void handle(Context ctx) {
        String cookieValue = ctx.cookie("cart");
        if (cookieValue == null) {
            ctx.status(HttpStatus.BAD_REQUEST);
            return;
        }
        removeJewelFromCart.handle(new RemoveJewelFromCart.Command(
                new CartId(cookieValue),
                new JewelId(ctx.pathParam("jewel-id"))
        ));
        redirectOrHtmx(ctx);
    }

    private void redirectOrHtmx(Context ctx) {
        if (ctx.header("HX-Request") != null) {
            ctx.header("HX-Redirect", "/cart");
            ctx.status(HttpStatus.NO_CONTENT);
        } else {
            ctx.redirect("/cart", HttpStatus.SEE_OTHER);
        }
    }

}