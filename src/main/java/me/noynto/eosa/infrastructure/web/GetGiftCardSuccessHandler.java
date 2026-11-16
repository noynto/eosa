package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ConfirmGiftCardPayment;
import me.noynto.eosa.shared.CheckoutSessionId;

import java.util.HashMap;
import java.util.Map;

/**
 * Retour de Stripe après l'achat d'une carte. Aucun code n'est affiché ici : il n'arrive que par e-mail.
 */
public record GetGiftCardSuccessHandler(
        ConfirmGiftCardPayment confirmGiftCardPayment,
        String baseUrl
) implements Handler {

    @Override
    public void handle(Context ctx) {
        var checkoutSessionId = new CheckoutSessionId();
        checkoutSessionId.setStripe(ctx.queryParam("session_id"));
        ConfirmGiftCardPayment.Result result;
        try {
            result = confirmGiftCardPayment.handle(new ConfirmGiftCardPayment.Command(checkoutSessionId));
        } catch (RuntimeException e) {
            ctx.status(404);
            return;
        }

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Eosa — Carte cadeau");
        model.put("description", "Confirmation de votre carte cadeau Eosa.");
        model.put("ogImageUrl", baseUrl + "/hero.webp");
        model.put("canonicalUrl", baseUrl + "/gift-cards/success");
        model.put("noindex", true);
        model.put("paid", result.paid());
        model.put("email", result.giftCard().getBuyerEmail());
        ctx.render("gift-card-success.mustache", model);
    }

}
