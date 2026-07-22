package me.noynto.eosa.application;

import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.product.Variant;
import me.noynto.eosa.shared.ProductId;
import me.noynto.eosa.shared.VariantId;

public record UpdateStockOfVariant(
        ProductProvider productProvider
) {

    public Product handle(Command command) {
        if (command.stock < 0) {
            throw new RuntimeException("Le stock du variant ne peut être négatif.");
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

        variant.setStock(command.stock);

        return this.productProvider.write(product);
    }

    public record Command(
            ProductId productId,
            VariantId variantId,
            int stock
    ) {
    }
}
