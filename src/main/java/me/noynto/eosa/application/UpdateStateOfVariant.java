package me.noynto.eosa.application;

import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.product.Variant;
import me.noynto.eosa.product.VariantState;
import me.noynto.eosa.shared.ProductId;
import me.noynto.eosa.shared.VariantId;

public record UpdateStateOfVariant(
        ProductProvider productProvider
) {

    public Product handle(Command command) {
        if (command.state == null) {
            throw new RuntimeException("L'état du variant est nécessaire.");
        }
        if (command.productId == null || command.productId.value() == null) {
            throw new RuntimeException("L'identifiant du produit est nécessaire.");
        }
        if (command.variantId == null || command.variantId.value() == null) {
            throw new RuntimeException("L'identifiant du variant est nécessaire.");
        }

        Product product = this.productProvider.read(command.productId)
                .orElseThrow(() -> new RuntimeException("Le produit " + command.productId.value() + " n'existe pas."));

        Variant variant = product.getVariants().stream()
                .filter(v -> v.getId().equals(command.variantId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Le variant " + command.variantId.value() + " n'existe pas sur ce produit."));

        if (variant.getState() == null) {
            variant.setState(VariantState.DRAFTED);
        }

        if (variant.getState() == command.state) {
            throw new RuntimeException("Le variant " + variant.getId().value() + " est déjà dans l'état demandé.");
        }

        if (variant.getState() == VariantState.PUBLISHED && command.state == VariantState.DRAFTED) {
            throw new RuntimeException("Un variant publié ne peut pas passer à nouveau brouillon.");
        }

        if (command.state == VariantState.PUBLISHED) {
            if (variant.getPrice() == null) {
                throw new RuntimeException("Le variant " + variant.getId().value() + " ne peut pas être publié, il lui faut un prix.");
            }
            if (variant.getImageIds() == null || variant.getImageIds().isEmpty()) {
                throw new RuntimeException("Le variant " + variant.getId().value() + " ne peut pas être publié, il lui faut au moins une image.");
            }
        }

        variant.setState(command.state);

        return this.productProvider.write(product);
    }

    public record Command(
            ProductId productId,
            VariantId variantId,
            VariantState state
    ) {
    }
}
