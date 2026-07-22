package me.noynto.eosa.application;

import me.noynto.eosa.charm.Charm;
import me.noynto.eosa.charm.CharmProvider;
import me.noynto.eosa.charm.CharmState;
import me.noynto.eosa.shared.CharmId;

public record UpdateStateOfCharm(
        CharmProvider charmProvider
) {

    public Charm handle(Command command) {
        if (command.state == null) {
            throw new RuntimeException("L'état de la breloque est nécessaire.");
        }
        if (command.charmId == null || command.charmId.value() == null) {
            throw new RuntimeException("L'identifiant de la breloque est nécessaire.");
        }

        Charm charm = this.charmProvider.read(command.charmId)
                .orElseThrow(() -> new RuntimeException("La breloque " + command.charmId.value() + " n'existe pas."));

        if (charm.getState() == null) {
            charm.setState(CharmState.DRAFTED);
        }

        if (charm.getState() == command.state) {
            throw new RuntimeException("La breloque " + charm.getId().value() + " est déjà dans l'état demandé.");
        }

        if (charm.getState() == CharmState.PUBLISHED && command.state == CharmState.DRAFTED) {
            throw new RuntimeException("Une breloque publiée ne peut pas repasser à nouveau brouillon.");
        }

        if (command.state == CharmState.PUBLISHED) {
            if (charm.getName() == null || charm.getName().isBlank()) {
                throw new RuntimeException("La breloque " + charm.getId().value() + " ne peut pas être publiée, il lui faut un nom.");
            }
            if (charm.getAdditionalPrice() == null) {
                throw new RuntimeException("La breloque " + charm.getId().value() + " ne peut pas être publiée, il lui faut un supplément de prix.");
            }
            if (charm.getImageId() == null) {
                throw new RuntimeException("La breloque " + charm.getId().value() + " ne peut pas être publiée, il lui faut une image.");
            }
        }

        charm.setState(command.state);

        return this.charmProvider.write(charm);
    }

    public record Command(
            CharmId charmId,
            CharmState state
    ) {
    }
}
