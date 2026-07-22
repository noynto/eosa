package me.noynto.eosa.application;

import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.shared.ProductId;
import me.noynto.eosa.shared.VariantId;

public record SetDefaultVariantOfProduct(
        ProductProvider productProvider
) {

    public Product handle(Command command) {
        if (command.productId == null || command.productId.value() == null) {
            throw new RuntimeException("L'identifiant du produit est nécessaire.");
        }
        if (command.variantId == null || command.variantId.value() == null) {
            throw new RuntimeException("L'identifiant du variant est nécessaire.");
        }

        Product product = this.productProvider.read(command.productId)
                .orElseThrow(() -> new RuntimeException("Le produit " + command.productId.value() + " n'existe pas."));

        boolean variantExists = product.getVariants().stream()
                .anyMatch(v -> v.getId().equals(command.variantId));
        if (!variantExists) {
            throw new RuntimeException("Le variant " + command.variantId.value() + " n'existe pas sur ce produit.");
        }

        product.setDefaultVariantId(command.variantId);

        return this.productProvider.write(product);
    }

    public record Command(
            ProductId productId,
            VariantId variantId
    ) {
    }
}
