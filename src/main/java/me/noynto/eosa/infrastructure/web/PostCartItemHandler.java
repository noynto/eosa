package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import me.noynto.eosa.application.AddJewelToCart;
import me.noynto.eosa.application.GetOrCreateCart;
import me.noynto.eosa.shared.CartId;
import me.noynto.eosa.shared.JewelId;

public record PostCartItemHandler(
        GetOrCreateCart getOrCreateCart,
        AddJewelToCart addJewelToCart
) implements Handler {

    @Override
    public void handle(Context ctx) {
        String cookieValue = ctx.cookie("cart");
        var cart = getOrCreateCart.handle(new GetOrCreateCart.Command(
                cookieValue != null ? new CartId(cookieValue) : null
        ));
        ctx.cookie("cart", cart.getId().value());
        addJewelToCart.handle(new AddJewelToCart.Command(
                cart.getId(),
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