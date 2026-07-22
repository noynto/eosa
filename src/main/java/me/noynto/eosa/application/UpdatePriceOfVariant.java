package me.noynto.eosa.application;

import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.product.Variant;
import me.noynto.eosa.shared.ProductId;
import me.noynto.eosa.shared.VariantId;

import java.math.BigDecimal;

public record UpdatePriceOfVariant(
        ProductProvider productProvider
) {

    public Product handle(Command command) {
        if (command.price == null) {
            throw new RuntimeException("Le prix du variant est nécessaire.");
        }
        if (command.price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Le prix du variant ne peut être inférieur ou égal à 0.");
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

        variant.setPrice(command.price);

        return this.productProvider.write(product);
    }

    public record Command(
            ProductId productId,
            VariantId variantId,
            BigDecimal price
    ) {
    }
}
