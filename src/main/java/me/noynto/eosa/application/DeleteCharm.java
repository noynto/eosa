package me.noynto.eosa.application;

import me.noynto.eosa.charm.CharmProvider;
import me.noynto.eosa.shared.CharmId;

public record DeleteCharm(
        CharmProvider charmProvider
) {

    public void handle(Command command) {
        if (command.charmId == null || command.charmId.value() == null) {
            throw new RuntimeException("L'identifiant de la breloque est nécessaire.");
        }
        charmProvider.delete(command.charmId);
    }

    public record Command(
            CharmId charmId
    ) {
    }

}
