package me.noynto.eosa.application;

import me.noynto.eosa.image.Image;
import me.noynto.eosa.image.ImageProvider;
import me.noynto.eosa.jewel.Jewel;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.JewelId;

import java.util.ArrayList;
import java.util.List;

public record AddImagesToJewel(
        JewelProvider jewelProvider,
        ImageProvider imageProvider
) {

    public Result handle(Command command) {
        // 1. Vérification
        if (command.jewelId == null || command.jewelId.value() == null) {
            throw new RuntimeException("L'identifiant du produit sur lequel ajouter une ou des images est nécessaire.");
        }
        if (command.images == null || command.images.isEmpty()) {
            throw new RuntimeException("Au moins un identifiant d'une image est nécessaire.");
        }
        for (Image image : command.images()) {
            if (!Image.ACCEPTED_FORMATS.contains(image.getFormat())) {
                throw new RuntimeException("Le format de l'image " + image.getName() + " n'est pas accepté : JPEG, PNG, WebP ou AVIF.");
            }
        }

        // 2. Résolution du produit
        Jewel jewel = jewelProvider.read(command.jewelId())
                .orElseThrow(() -> new RuntimeException("Le produit " + command.jewelId.value() + " sur lequel ajouter une ou des images n'existe pas."));

        // 3. Création d'une copie des images déjà présentes.
        List<ImageId> imageIds = new ArrayList<>(jewel.getImageIds());

        for (Image image : command.images()) {
            Image uploaded = imageProvider.upload(image);
            imageIds.add(uploaded.getId());
        }

        // 4. Modification des identifiants d'image sur le produit
        jewel.setImageIds(imageIds);

        // 5. Enregistrement du produit chez le fournisseur.
        return new Result(jewelProvider.write(jewel));
    }

    public record Result(
            Jewel jewel
    ) {
    }

    public record Command(
            JewelId jewelId,
            List<Image> images
    ) {
    }

}