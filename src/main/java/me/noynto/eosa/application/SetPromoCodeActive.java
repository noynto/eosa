package me.noynto.eosa.application;

import me.noynto.eosa.promo.PromoCode;
import me.noynto.eosa.promo.PromoCodeProvider;
import me.noynto.eosa.shared.PromoCodeId;

/**
 * Active ou désactive un code. Un code désactivé n'est plus accepté dans le panier mais reste lisible,
 * avec ses commandes.
 */
public record SetPromoCodeActive(
        PromoCodeProvider promoCodeProvider
) {

    public Result handle(Command command) {
        if (command.promoCodeId == null || command.promoCodeId.value() == null) {
            throw new RuntimeException("L'identifiant du code est nécessaire.");
        }

        PromoCode promoCode = promoCodeProvider.read(command.promoCodeId)
                .orElseThrow(() -> new RuntimeException("Le code " + command.promoCodeId.value() + " n'existe pas."));

        promoCode.setActive(command.active);
        return new Result(promoCodeProvider.write(promoCode));
    }

    public record Result(
            PromoCode promoCode
    ) {
    }

    public record Command(
            PromoCodeId promoCodeId,
            boolean active
    ) {
    }

}
