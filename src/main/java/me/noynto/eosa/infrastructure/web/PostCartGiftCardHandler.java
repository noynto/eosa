package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import me.noynto.eosa.application.ApplyGiftCardToCart;
import me.noynto.eosa.shared.CartId;

/**
 * Applique le code de carte cadeau saisi dans le panier ; un refus s'affiche sous le champ (htmx, 422). Les essais
 * sont limités par adresse IP.
 */
public record PostCartGiftCardHandler(
        ApplyGiftCardToCart applyGiftCardToCart,
        RequestRateLimiter requestRateLimiter
) implements Handler {

    @Override
    public void handle(Context ctx) {
        // Posé par EnsureCartHandler, qui a déjà créé le panier au besoin.
        CartId cartId = ctx.attribute("cartId");
        if (!requestRateLimiter.tryAcquire(RequestRateLimiter.clientOf(ctx))) {
            InlineError.render(ctx, "Trop d'essais depuis votre connexion. Réessayez dans une heure.");
            return;
        }
        try {
            applyGiftCardToCart.handle(new ApplyGiftCardToCart.Command(cartId, ctx.formParam("code")));
        } catch (RuntimeException e) {
            InlineError.render(ctx, e);
            return;
        }
        ctx.header("HX-Redirect", "/cart");
        ctx.status(HttpStatus.NO_CONTENT);
    }

}
