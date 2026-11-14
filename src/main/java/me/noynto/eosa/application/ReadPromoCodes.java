package me.noynto.eosa.application;

import me.noynto.eosa.promo.PromoCode;
import me.noynto.eosa.promo.PromoCodeProvider;

import java.util.List;

public record ReadPromoCodes(
        PromoCodeProvider promoCodeProvider
) {

    public Result handle() {
        return new Result(promoCodeProvider.readIds(null)
                .map(id -> promoCodeProvider.read(id).orElseThrow())
                .toList());
    }

    public record Result(
            List<PromoCode> promoCodes
    ) {
    }

}
