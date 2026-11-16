package me.noynto.eosa.application;

import me.noynto.eosa.order.OrderProvider;
import me.noynto.eosa.promo.PromoCode;
import me.noynto.eosa.promo.PromoCodeProvider;

import java.util.List;

/** Les codes de promotion avec le nombre de commandes passées avec chacun. */
public record ReadPromoCodes(
        PromoCodeProvider promoCodeProvider,
        OrderProvider orderProvider
) {

    public Result handle() {
        return new Result(promoCodeProvider.readIds(null)
                .map(id -> promoCodeProvider.read(id).orElseThrow())
                .map(promoCode -> new Entry(promoCode,
                        (int) orderProvider.readIds(null, null, null, promoCode.getId()).count()))
                .toList());
    }

    public record Result(
            List<Entry> entries
    ) {
    }

    public record Entry(
            PromoCode promoCode,
            int uses
    ) {
    }

}
