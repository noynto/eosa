package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.GetOrCreateCart;
import me.noynto.eosa.shared.CartId;

public record EnsureCartHandler(GetOrCreateCart getOrCreateCart) implements Handler {

    @Override
    public void handle(Context ctx) {
        String cookieValue = ctx.cookie("cart");
        var cart = getOrCreateCart.handle(new GetOrCreateCart.Command(
                cookieValue != null ? new CartId(cookieValue) : null
        ));
        ctx.cookie("cart", cart.getId().value());
    }

}