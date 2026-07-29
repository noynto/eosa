package me.noynto.eosa.application;

import me.noynto.eosa.jewel.Jewel;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.shared.JewelId;

import java.math.BigDecimal;

public record UpdatePriceOfJewel(
        JewelProvider jewelProvider
) {

    public Jewel handle(Command command) {
        // 1. Vérification
        if (command.price == null) {
            throw new RuntimeException("Le prix du produit est nécessaire.");
        }
        if (command.price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Le prix du produit ne peut être inférieur ou égale à 0.");
        }
        if (command.jewelId == null || command.jewelId.value() == null) {
            throw new RuntimeException("L'identifiant du produit sur lequel mettre à jour l'accroche est nécessaire.");
        }

        // 2. Résolution du produit concerné.
        Jewel jewel = this.jewelProvider.read(command.jewelId)
                .orElseThrow(() -> new RuntimeException("Le produit " + command.jewelId.value() + " sur lequel mettre à jour l'accroche n'existe pas."));

        // 3. Modification de l'accroche sur le produit
        jewel.setPrice(command.price);

        // 4. Enregistrer le produit chez le fournisseur
        return this.jewelProvider.write(jewel);
    }

    public record Command(
            JewelId jewelId,
            BigDecimal price
    ) {
    }
}
