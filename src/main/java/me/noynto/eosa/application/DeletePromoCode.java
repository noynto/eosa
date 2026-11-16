package me.noynto.eosa.application;

import me.noynto.eosa.order.OrderProvider;
import me.noynto.eosa.promo.PromoCodeProvider;
import me.noynto.eosa.shared.PromoCodeId;

/**
 * Supprime un code qui n'a jamais servi. Un code utilisé par une commande (même annulée) ne se supprime pas :
 * il se désactive, pour que l'historique des commandes garde son sens.
 */
public record DeletePromoCode(
        PromoCodeProvider promoCodeProvider,
        OrderProvider orderProvider
) {

    public void handle(Command command) {
        if (command.promoCodeId == null || command.promoCodeId.value() == null) {
            throw new RuntimeException("L'identifiant du code est nécessaire.");
        }
        promoCodeProvider.read(command.promoCodeId)
                .orElseThrow(() -> new RuntimeException("Le code " + command.promoCodeId.value() + " n'existe pas."));
        if (orderProvider.readIds(null, null, null, command.promoCodeId).findAny().isPresent()) {
            throw new RuntimeException("Ce code a déjà servi : désactivez-le plutôt que de le supprimer.");
        }
        promoCodeProvider.delete(command.promoCodeId);
    }

    public record Command(
            PromoCodeId promoCodeId
    ) {
    }

}
