package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadGiftCard;
import me.noynto.eosa.giftcard.GiftCardMovementKind;
import me.noynto.eosa.giftcard.GiftCardState;
import me.noynto.eosa.shared.GiftCardId;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

public record GetAdminGiftCardHandler(ReadGiftCard readGiftCard, Clock clock) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            var result = readGiftCard.handle(new ReadGiftCard.Command(new GiftCardId(ctx.pathParam("id"))));
            var giftCard = result.giftCard();
            var now = clock.instant();
            var orders = result.orders().stream().map(order -> {
                Map<String, Object> row = new HashMap<>();
                row.put("id", order.getId().value());
                row.put("number", order.getNumber());
                row.put("paidAt", OrderFormat.dateTime(order.getPaidAt()));
                row.put("amount", OrderFormat.amount(order.getGiftCardAmount()));
                row.put("state", OrderFormat.state(order.getState()));
                return row;
            }).toList();
            var movements = giftCard.getMovements().stream().map(movement -> {
                Map<String, Object> row = new HashMap<>();
                row.put("kind", GiftCardFormat.movementKind(movement.getKind()));
                row.put("sign", GiftCardFormat.movementSign(movement.getKind()));
                row.put("amount", OrderFormat.amount(movement.getAmount()));
                row.put("noEffect", movement.getKind() == GiftCardMovementKind.USED);
                row.put("date", OrderFormat.dateTime(movement.getCreatedAt()));
                row.put("hasReason", movement.getReason() != null);
                row.put("reason", movement.getReason());
                return row;
            }).toList();

            Map<String, Object> model = new HashMap<>();
            model.put("title", "Carte " + GiftCardFormat.maskedCode(giftCard));
            model.put("giftCardId", giftCard.getId().value());
            model.put("code", giftCard.getCode());
            model.put("status", GiftCardFormat.status(giftCard, now));
            model.put("initialAmount", OrderFormat.amount(giftCard.getInitialAmount()));
            model.put("balance", OrderFormat.amount(giftCard.balance()));
            model.put("validUntil", GiftCardFormat.validUntil(giftCard));
            model.put("createdAt", OrderFormat.dateTime(giftCard.getCreatedAt()));
            model.put("hasBuyerEmail", giftCard.getBuyerEmail() != null);
            model.put("buyerEmail", giftCard.getBuyerEmail());
            model.put("hasBuyerFirstName", giftCard.getBuyerFirstName() != null);
            model.put("buyerFirstName", giftCard.getBuyerFirstName());
            model.put("movements", movements);
            model.put("orders", orders);
            model.put("hasOrders", !orders.isEmpty());
            model.put("isActive", giftCard.getState() == GiftCardState.ACTIVE && !giftCard.isExpired(now));
            ctx.render("admin/gift-card.mustache", model);
        } catch (RuntimeException e) {
            ctx.status(404);
        }
    }

}
