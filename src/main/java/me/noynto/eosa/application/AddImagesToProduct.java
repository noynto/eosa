package me.noynto.eosa.application;

import me.noynto.eosa.image.Image;
import me.noynto.eosa.image.ImageProvider;
import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.ProductId;

import java.util.ArrayList;
import java.util.List;

public record AddImagesToProduct(
        ProductProvider productProvider,
        ImageProvider imageProvider
) {

    public Product handle(Command command) {
        // 1. Vérification
        if (command.productId == null || command.productId.value() == null) {
            throw new RuntimeException("L'identifiant du produit sur lequel ajouter une ou des images est nécessaire.");
        }
        if (command.images == null || command.images.isEmpty()) {
            throw new RuntimeException("Au moins un identifiant d'une image est nécessaire.");
        }

        // 2. Résolution du produit
        Product product = productProvider.read(command.productId())
                .orElseThrow(() -> new RuntimeException("Le produit " + command.productId.value() + " sur lequel ajouter une ou des images n'existe pas."));

        // 3. Création d'une copie des images déjà présentes.
        List<ImageId> imageIds = new ArrayList<>(product.getImageIds());

        for (Image image : command.images()) {
            Image uploaded = imageProvider.upload(image);
            imageIds.add(uploaded.getId());
        }

        // 4. Modification des identifiants d'image sur le produit
        product.setImageIds(imageIds);

        // 5. Enregistrement du produit chez le fournisseur.
        return productProvider.write(product);
    }

    public record Command(
            ProductId productId,
            List<Image> images
    ) {
    }

}