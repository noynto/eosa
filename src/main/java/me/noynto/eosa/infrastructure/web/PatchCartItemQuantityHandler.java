package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import me.noynto.eosa.application.UpdateCartItemQuantity;
import me.noynto.eosa.shared.CartId;
import me.noynto.eosa.shared.VariantId;

public record PatchCartItemQuantityHandler(
        UpdateCartItemQuantity updateCartItemQuantity
) implements Handler {

    @Override
    public void handle(Context ctx) {
        String cookieValue = ctx.cookie("cart");
        if (cookieValue == null) {
            ctx.status(HttpStatus.BAD_REQUEST);
            return;
        }
        int quantity;
        try {
            quantity = Integer.parseInt(ctx.formParam("quantity"));
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            return;
        }
        updateCartItemQuantity.handle(new UpdateCartItemQuantity.Command(
                new CartId(cookieValue),
                new VariantId(ctx.pathParam("product-id")),
                quantity
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