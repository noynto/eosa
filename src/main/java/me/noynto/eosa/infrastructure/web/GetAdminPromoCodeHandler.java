package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadPromoCode;
import me.noynto.eosa.promo.PromoCodeType;
import me.noynto.eosa.shared.PromoCodeId;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

public record GetAdminPromoCodeHandler(ReadPromoCode readPromoCode, Clock clock) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            var promoCode = readPromoCode.handle(new ReadPromoCode.Command(new PromoCodeId(ctx.pathParam("id")))).promoCode();
            Map<String, Object> model = new HashMap<>();
            model.put("title", promoCode.getCode());
            model.put("promoCodeId", promoCode.getId().value());
            model.put("code", promoCode.getCode());
            model.put("status", PromoCodeFormat.status(promoCode, clock.instant()));
            model.put("active", promoCode.isActive());
            model.put("isPercent", promoCode.getType() == PromoCodeType.PERCENT);
            model.put("isAmount", promoCode.getType() == PromoCodeType.AMOUNT);
            model.put("value", PromoCodeFormat.inputValue(promoCode));
            model.put("startsOn", PromoCodeFormat.startsOn(promoCode));
            model.put("endsOn", PromoCodeFormat.endsOn(promoCode));
            model.put("maxUses", promoCode.getMaxUses() == null ? "" : promoCode.getMaxUses());
            model.put("minimumAmount", promoCode.getMinimumAmount() == null ? "" : promoCode.getMinimumAmount().toPlainString());
            model.put("createdAt", OrderFormat.dateTime(promoCode.getCreatedAt()));
            ctx.render("admin/promo-code.mustache", model);
        } catch (RuntimeException e) {
            ctx.status(404);
        }
    }

}
