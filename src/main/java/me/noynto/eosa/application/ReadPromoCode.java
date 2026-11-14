package me.noynto.eosa.application;

import me.noynto.eosa.promo.PromoCode;
import me.noynto.eosa.promo.PromoCodeProvider;
import me.noynto.eosa.shared.PromoCodeId;

public record ReadPromoCode(
        PromoCodeProvider promoCodeProvider
) {

    public Result handle(Command command) {
        if (command.promoCodeId == null || command.promoCodeId.value() == null) {
            throw new RuntimeException("L'identifiant du code est nécessaire.");
        }
        return new Result(promoCodeProvider.read(command.promoCodeId)
                .orElseThrow(() -> new RuntimeException("Le code " + command.promoCodeId.value() + " n'existe pas.")));
    }

    public record Result(
            PromoCode promoCode
    ) {
    }

    public record Command(
            PromoCodeId promoCodeId
    ) {
    }

}
