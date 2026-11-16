package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import me.noynto.eosa.application.RemovePromoCode;
import me.noynto.eosa.shared.CartId;

public record DeleteCartPromoCodeHandler(RemovePromoCode removePromoCode) implements Handler {

    @Override
    public void handle(Context ctx) {
        // Posé par EnsureCartHandler, qui a déjà créé le panier au besoin.
        CartId cartId = ctx.attribute("cartId");
        removePromoCode.handle(new RemovePromoCode.Command(cartId));
        ctx.header("HX-Redirect", "/cart");
        ctx.status(HttpStatus.NO_CONTENT);
    }

}
