package me.noynto.eosa.application;

import me.noynto.eosa.image.Image;
import me.noynto.eosa.image.ImageProvider;
import me.noynto.eosa.metal.MetalColor;
import me.noynto.eosa.metal.MetalColorProvider;
import me.noynto.eosa.shared.MetalColorId;

public record AddImageToMetalColor(
        MetalColorProvider metalColorProvider,
        ImageProvider imageProvider
) {

    public MetalColor handle(Command command) {
        if (command.metalColorId == null || command.metalColorId.value() == null) {
            throw new RuntimeException("L'identifiant de la couleur sur laquelle ajouter une image est nécessaire.");
        }
        if (command.image == null) {
            throw new RuntimeException("Une image est nécessaire.");
        }

        MetalColor metalColor = metalColorProvider.read(command.metalColorId())
                .orElseThrow(() -> new RuntimeException("La couleur " + command.metalColorId.value() + " n'existe pas."));

        Image uploaded = imageProvider.upload(command.image());
        metalColor.setImageId(uploaded.getId());

        return metalColorProvider.write(metalColor);
    }

    public record Command(
            MetalColorId metalColorId,
            Image image
    ) {
    }

}
