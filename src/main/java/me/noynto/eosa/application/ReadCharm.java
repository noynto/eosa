package me.noynto.eosa.application;

import me.noynto.eosa.charm.Charm;
import me.noynto.eosa.charm.CharmProvider;
import me.noynto.eosa.shared.CharmId;

public record ReadCharm(
        CharmProvider charmProvider
) {

    public Charm handle(Command command) {
        if (command.charmId == null || command.charmId.value() == null) {
            throw new RuntimeException("L'identifiant de la breloque est nécessaire.");
        }
        return charmProvider.read(command.charmId())
                .orElseThrow(() -> new RuntimeException("La breloque " + command.charmId.value() + " n'existe pas."));
    }

    public record Command(
            CharmId charmId
    ) {
    }

}
