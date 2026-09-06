package me.noynto.eosa.application;

import me.noynto.eosa.charm.Charm;
import me.noynto.eosa.charm.CharmProvider;
import me.noynto.eosa.shared.CharmId;

import java.math.BigDecimal;

public record UpdateCharm(
        CharmProvider charmProvider
) {

    public Charm handle(Command command) {
        if (command.charmId == null || command.charmId.value() == null) {
            throw new RuntimeException("L'identifiant de la breloque est nécessaire.");
        }
        if (command.name == null || command.name.isBlank()) {
            throw new RuntimeException("Le nom de la breloque est requis.");
        }
        if (command.price == null || command.price.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Le prix de la breloque est requis et ne peut pas être négatif.");
        }

        Charm charm = charmProvider.read(command.charmId)
                .orElseThrow(() -> new RuntimeException("La breloque " + command.charmId.value() + " n'existe pas."));

        charm.setName(command.name());
        charm.setPrice(command.price());
        return charmProvider.write(charm);
    }

    public record Command(
            CharmId charmId,
            String name,
            BigDecimal price
    ) {
    }

}
