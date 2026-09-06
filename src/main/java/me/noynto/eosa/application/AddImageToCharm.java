package me.noynto.eosa.application;

import me.noynto.eosa.charm.Charm;
import me.noynto.eosa.charm.CharmProvider;
import me.noynto.eosa.image.Image;
import me.noynto.eosa.image.ImageProvider;
import me.noynto.eosa.shared.CharmId;

public record AddImageToCharm(
        CharmProvider charmProvider,
        ImageProvider imageProvider
) {

    public Charm handle(Command command) {
        if (command.charmId == null || command.charmId.value() == null) {
            throw new RuntimeException("L'identifiant de la breloque sur laquelle ajouter une image est nécessaire.");
        }
        if (command.image == null) {
            throw new RuntimeException("Une image est nécessaire.");
        }

        Charm charm = charmProvider.read(command.charmId())
                .orElseThrow(() -> new RuntimeException("La breloque " + command.charmId.value() + " n'existe pas."));

        Image uploaded = imageProvider.upload(command.image());
        charm.setImageId(uploaded.getId());

        return charmProvider.write(charm);
    }

    public record Command(
            CharmId charmId,
            Image image
    ) {
    }

}
