package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadGiftCardByToken;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

/**
 * La page imprimable d'une carte, ouverte par son jeton (jamais par son code, jamais indexée).
 */
public record GetGiftCardHandler(
        ReadGiftCardByToken readGiftCardByToken,
        Clock clock,
        String baseUrl
) implements Handler {

    @Override
    public void handle(Context ctx) {
        ReadGiftCardByToken.Result result;
        try {
            result = readGiftCardByToken.handle(new ReadGiftCardByToken.Command(ctx.pathParam("token")));
        } catch (RuntimeException e) {
            ctx.status(404);
            return;
        }
        var giftCard = result.giftCard();
        Map<String, Object> model = new HashMap<>();
        model.put("title", "Eosa — Votre carte cadeau");
        model.put("description", "Votre carte cadeau Eosa.");
        model.put("ogImageUrl", baseUrl + "/hero.webp");
        model.put("canonicalUrl", baseUrl + "/gift-cards");
        model.put("noindex", true);
        model.put("code", giftCard.getCode());
        model.put("initialAmount", OrderFormat.amount(giftCard.getInitialAmount()));
        model.put("balance", OrderFormat.amount(giftCard.balance()));
        model.put("validUntil", GiftCardFormat.validUntil(giftCard));
        model.put("status", GiftCardFormat.status(giftCard, clock.instant()));
        model.put("usable", result.usable());
        ctx.render("gift-card.mustache", model);
    }

}
