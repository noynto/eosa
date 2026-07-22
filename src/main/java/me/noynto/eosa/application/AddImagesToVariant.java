package me.noynto.eosa.application;

import me.noynto.eosa.image.Image;
import me.noynto.eosa.image.ImageProvider;
import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.product.Variant;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.ProductId;
import me.noynto.eosa.shared.VariantId;

import java.util.ArrayList;
import java.util.List;

public record AddImagesToVariant(
        ProductProvider productProvider,
        ImageProvider imageProvider
) {

    public Product handle(Command command) {
        if (command.productId == null || command.productId.value() == null) {
            throw new RuntimeException("L'identifiant du produit est nécessaire.");
        }
        if (command.variantId == null || command.variantId.value() == null) {
            throw new RuntimeException("L'identifiant du variant sur lequel ajouter une ou des images est nécessaire.");
        }
        if (command.images == null || command.images.isEmpty()) {
            throw new RuntimeException("Au moins un identifiant d'une image est nécessaire.");
        }

        Product product = productProvider.read(command.productId())
                .orElseThrow(() -> new RuntimeException("Le produit " + command.productId.value() + " n'existe pas."));

        Variant variant = product.getVariants().stream()
                .filter(v -> v.getId().equals(command.variantId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Le variant " + command.variantId.value() + " n'existe pas sur ce produit."));

        List<ImageId> imageIds = new ArrayList<>(variant.getImageIds());
        for (Image image : command.images()) {
            Image uploaded = imageProvider.upload(image);
            imageIds.add(uploaded.getId());
        }
        variant.setImageIds(imageIds);

        return productProvider.write(product);
    }

    public record Command(
            ProductId productId,
            VariantId variantId,
            List<Image> images
    ) {
    }

}
