package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.GetOrCreateCart;
import me.noynto.eosa.cart.Cart;
import me.noynto.eosa.shared.CartId;

import java.util.Map;

public record GetCartHandler(GetOrCreateCart getOrCreateCart) implements Handler {

    @Override
    public void handle(Context ctx) {
        String cookieValue = ctx.cookie("cart");
        Cart cart = getOrCreateCart.handle(new GetOrCreateCart.Command(
                cookieValue != null ? new CartId(cookieValue) : null
        ));
        ctx.cookie("cart", cart.getId().value());
        ctx.render("cart.jte", Map.of("cart", cart));
    }

}