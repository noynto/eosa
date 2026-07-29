package me.noynto.eosa.application;

import me.noynto.eosa.jewel.Jewel;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.shared.JewelId;

public record ReadJewel(
        JewelProvider jewelProvider
) {

    public Jewel handle(Command command) {
        // 1. Vérification de la commande.
        if (command.jewelId == null || command.jewelId.value() == null) {
            throw new RuntimeException("L'identifiant du produit sur lequel mettre à jour l'accroche est nécessaire.");
        }
        // 2. Résolution du produit concerné.
        return jewelProvider.read(command.jewelId())
                .orElseThrow(() -> new RuntimeException("Le produit " + command.jewelId.value() + " n'existe pas."));
    }

    public record Command(
            JewelId jewelId
    ) {
    }

}
