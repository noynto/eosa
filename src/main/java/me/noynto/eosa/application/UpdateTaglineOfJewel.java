package me.noynto.eosa.application;

import me.noynto.eosa.jewel.Jewel;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.shared.JewelId;

public record UpdateTaglineOfJewel(
        JewelProvider jewelProvider
) {

    public Jewel handle(Command command) {
        // 1. Vérification
        if (command.tagline == null) {
            throw new RuntimeException("L'accroche du produit est nécessaire.");
        }
        if (command.tagline.isBlank()) {
            throw new RuntimeException("L'accroche du produit ne peut être vide.");
        }
        if (command.jewelId == null || command.jewelId.value() == null) {
            throw new RuntimeException("L'identifiant du produit sur lequel mettre à jour l'accroche est nécessaire.");
        }

        // 2. Résolution du produit concerné.
        Jewel jewel = this.jewelProvider.read(command.jewelId)
                .orElseThrow(() -> new RuntimeException("Le produit " + command.jewelId.value() + " sur lequel mettre à jour l'accroche n'existe pas."));

        // 3. Modification de l'accroche sur le produit
        jewel.setTagline(command.tagline);

        // 4. Enregistrer le produit chez le fournisseur
        return this.jewelProvider.write(jewel);
    }

    public record Command(
            JewelId jewelId,
            String tagline
    ) {
    }
}
