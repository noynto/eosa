package me.noynto.eosa.application;

import me.noynto.eosa.charm.Charm;
import me.noynto.eosa.charm.CharmProvider;
import me.noynto.eosa.shared.CharmId;

import java.math.BigDecimal;

public record UpdateAdditionalPriceOfCharm(
        CharmProvider charmProvider
) {

    public Charm handle(Command command) {
        if (command.additionalPrice == null) {
            throw new RuntimeException("Le supplément de prix de la breloque est nécessaire.");
        }
        if (command.additionalPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Le supplément de prix de la breloque ne peut être négatif.");
        }
        if (command.charmId == null || command.charmId.value() == null) {
            throw new RuntimeException("L'identifiant de la breloque est nécessaire.");
        }

        Charm charm = this.charmProvider.read(command.charmId)
                .orElseThrow(() -> new RuntimeException("La breloque " + command.charmId.value() + " n'existe pas."));

        charm.setAdditionalPrice(command.additionalPrice);

        return this.charmProvider.write(charm);
    }

    public record Command(
            CharmId charmId,
            BigDecimal additionalPrice
    ) {
    }
}
