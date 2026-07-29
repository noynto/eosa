package me.noynto.eosa.application;

import me.noynto.eosa.jewel.Jewel;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.jewel.JewelState;
import me.noynto.eosa.shared.JewelId;

public record UpdateStateOfJewel(
        JewelProvider jewelProvider
) {

    public Jewel handle(Command command) {
        // 1. Vérification
        if (command.state == null) {
            throw new RuntimeException("L'état du produit est nécessaire.");
        }
        if (command.jewelId == null || command.jewelId.value() == null) {
            throw new RuntimeException("L'identifiant du produit sur lequel mettre à jour l'accroche est nécessaire.");
        }

        // 2. Résolution du produit concerné.
        Jewel jewel = this.jewelProvider.read(command.jewelId)
                .orElseThrow(() -> new RuntimeException("Le produit " + command.jewelId.value() + " sur lequel mettre à jour l'accroche n'existe pas."));

        if (jewel.getState() == null) {
            jewel.setState(JewelState.DRAFTED);
        }

        if (jewel.getState() == command.state) {
            throw new RuntimeException("Le produit " + jewel.getId().value() + " est déjà dans l'état demandé.");
        }

        if (jewel.getState() == JewelState.PUBLISHED && command.state == JewelState.DRAFTED) {
            throw new RuntimeException("Un produit publié ne peut pas passer à nouveau brouillon.");
        }

        if (command.state == JewelState.PUBLISHED) {
            if (jewel.getName() == null || jewel.getName().isBlank()) {
                throw new RuntimeException("Le produit " + jewel.getId().value() + " ne peut pas être publié, il lui faut un nom.");
            }
            if (jewel.getCategory() == null) {
                throw new RuntimeException("Le produit " + jewel.getId().value() + " ne peut pas être publié, il lui faut une catégorie.");
            }
            if (jewel.getTagline() == null) {
                throw new RuntimeException("Le produit " + jewel.getId().value() + " ne peut pas être publié, il lui faut une accroche.");
            }
            if (jewel.getPrice() == null) {
                throw new RuntimeException("Le produit " + jewel.getId().value() + " ne peut pas être publié, il lui faut un prix.");
            }
            if (jewel.getImageIds() == null || jewel.getImageIds().isEmpty()) {
                throw new RuntimeException("Le produit " + jewel.getId().value() + " ne peut pas être publié, il lui faut au moins une image.");
            }
        }

        // 3. Modification de l'état sur le produit
        jewel.setState(command.state);

        // 4. Enregistrer le produit chez le fournisseur
        return this.jewelProvider.write(jewel);
    }

    public record Command(
            JewelId jewelId,
            JewelState state
    ) {
    }
}
