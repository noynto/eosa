package me.noynto.eosa.application;

import me.noynto.eosa.jewel.Jewel;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.shared.JewelId;

public record UpdateNameOfJewel(
        JewelProvider jewelProvider
) {

    public Result handle(Command command) {
        // 1. Vérification
        if (command.name == null) {
            throw new RuntimeException("Le nom du bijou est nécessaire.");
        }
        if (command.name.isBlank()) {
            throw new RuntimeException("Le nom du bijou ne peut être vide.");
        }
        if (command.jewelId == null || command.jewelId.value() == null) {
            throw new RuntimeException("L'identifiant du bijou à renommer est nécessaire.");
        }

        // 2. Résolution du bijou concerné.
        Jewel jewel = this.jewelProvider.read(command.jewelId)
                .orElseThrow(() -> new RuntimeException("Le bijou " + command.jewelId.value() + " à renommer n'existe pas."));

        // 3. Modification du nom (sans les espaces autour)
        jewel.setName(command.name.strip());

        // 4. Enregistrer le bijou chez le fournisseur
        return new Result(this.jewelProvider.write(jewel));
    }

    public record Result(
            Jewel jewel
    ) {
    }

    public record Command(
            JewelId jewelId,
            String name
    ) {
    }
}
