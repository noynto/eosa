package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import me.noynto.eosa.application.ApplyPromoCode;
import me.noynto.eosa.shared.CartId;

/** Applique le code saisi dans le panier ; un refus s'affiche sous le champ (htmx, 422). */
public record PostCartPromoCodeHandler(ApplyPromoCode applyPromoCode) implements Handler {

    @Override
    public void handle(Context ctx) {
        // Posé par EnsureCartHandler, qui a déjà créé le panier au besoin.
        CartId cartId = ctx.attribute("cartId");
        try {
            applyPromoCode.handle(new ApplyPromoCode.Command(cartId, ctx.formParam("code")));
        } catch (RuntimeException e) {
            InlineError.render(ctx, e);
            return;
        }
        ctx.header("HX-Redirect", "/cart");
        ctx.status(HttpStatus.NO_CONTENT);
    }

}
