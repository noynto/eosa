package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadPromoCodes;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

public record GetAdminPromoCodesHandler(ReadPromoCodes readPromoCodes, Clock clock) implements Handler {

    @Override
    public void handle(Context ctx) {
        var now = clock.instant();
        var rows = readPromoCodes.handle().promoCodes().stream().map(promoCode -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", promoCode.getId().value());
            row.put("code", promoCode.getCode());
            row.put("discount", PromoCodeFormat.discount(promoCode));
            row.put("validity", PromoCodeFormat.validity(promoCode));
            row.put("status", PromoCodeFormat.status(promoCode, now));
            row.put("isActive", "Actif".equals(PromoCodeFormat.status(promoCode, now)));
            row.put("hasMaxUses", promoCode.getMaxUses() != null);
            row.put("maxUses", promoCode.getMaxUses());
            row.put("hasMinimum", promoCode.getMinimumAmount() != null);
            row.put("minimum", promoCode.getMinimumAmount() == null ? null : OrderFormat.amount(promoCode.getMinimumAmount()));
            return row;
        }).toList();

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Eosa — Codes de promotion");
        model.put("promoCodes", rows);
        model.put("hasPromoCodes", !rows.isEmpty());
        ctx.render("admin/promo-codes.mustache", model);
    }

}
