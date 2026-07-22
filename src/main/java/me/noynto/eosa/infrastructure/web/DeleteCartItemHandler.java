package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import me.noynto.eosa.application.RemoveVariantFromCart;
import me.noynto.eosa.shared.CartId;
import me.noynto.eosa.shared.VariantId;

public record DeleteCartItemHandler(
        RemoveVariantFromCart removeVariantFromCart
) implements Handler {

    @Override
    public void handle(Context ctx) {
        String cookieValue = ctx.cookie("cart");
        if (cookieValue == null) {
            ctx.status(HttpStatus.BAD_REQUEST);
            return;
        }
        removeVariantFromCart.handle(new RemoveVariantFromCart.Command(
                new CartId(cookieValue),
                new VariantId(ctx.pathParam("product-id"))
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
