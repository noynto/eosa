package me.noynto.eosa.application;

import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderProvider;
import me.noynto.eosa.promo.PromoCode;
import me.noynto.eosa.promo.PromoCodeProvider;
import me.noynto.eosa.shared.PromoCodeId;

import java.math.BigDecimal;
import java.util.List;

/** Un code de promotion, avec les commandes passées avec lui et la remise totale accordée. */
public record ReadPromoCode(
        PromoCodeProvider promoCodeProvider,
        OrderProvider orderProvider
) {

    public Result handle(Command command) {
        if (command.promoCodeId == null || command.promoCodeId.value() == null) {
            throw new RuntimeException("L'identifiant du code est nécessaire.");
        }
        PromoCode promoCode = promoCodeProvider.read(command.promoCodeId)
                .orElseThrow(() -> new RuntimeException("Le code " + command.promoCodeId.value() + " n'existe pas."));
        List<Order> orders = orderProvider.readIds(null, null, null, promoCode.getId())
                .map(id -> orderProvider.read(id).orElseThrow())
                .toList();
        BigDecimal totalDiscount = orders.stream()
                .map(Order::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Result(promoCode, orders, totalDiscount);
    }

    public record Result(
            PromoCode promoCode,
            List<Order> orders,
            BigDecimal totalDiscount
    ) {
    }

    public record Command(
            PromoCodeId promoCodeId
    ) {
    }

}
