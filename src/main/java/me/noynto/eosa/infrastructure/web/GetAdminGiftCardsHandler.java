package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadGiftCards;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

public record GetAdminGiftCardsHandler(ReadGiftCards readGiftCards, Clock clock) implements Handler {

    @Override
    public void handle(Context ctx) {
        var now = clock.instant();
        var rows = readGiftCards.handle().giftCards().stream().map(giftCard -> {
            var status = GiftCardFormat.status(giftCard, now);
            Map<String, Object> row = new HashMap<>();
            row.put("id", giftCard.getId().value());
            row.put("code", GiftCardFormat.maskedCode(giftCard));
            row.put("initialAmount", OrderFormat.amount(giftCard.getInitialAmount()));
            row.put("balance", OrderFormat.amount(giftCard.balance()));
            row.put("status", status);
            row.put("isActive", "Active".equals(status));
            row.put("buyerEmail", giftCard.getBuyerEmail());
            row.put("createdAt", OrderFormat.dateTime(giftCard.getCreatedAt()));
            row.put("validUntil", GiftCardFormat.validUntil(giftCard));
            return row;
        }).toList();

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Eosa — Cartes cadeau");
        model.put("giftCards", rows);
        model.put("hasGiftCards", !rows.isEmpty());
        ctx.render("admin/gift-cards.mustache", model);
    }

}
