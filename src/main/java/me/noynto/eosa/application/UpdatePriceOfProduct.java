package me.noynto.eosa.application;

import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.shared.ProductId;

import java.math.BigDecimal;

public record UpdatePriceOfProduct(
        ProductProvider productProvider
) {

    public Product handle(Command command) {
        // 1. Vérification
        if (command.price == null) {
            throw new RuntimeException("Le prix du produit est nécessaire.");
        }
        if (command.price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Le prix du produit ne peut être inférieur ou égale à 0.");
        }
        if (command.productId == null || command.productId.value() == null) {
            throw new RuntimeException("L'identifiant du produit sur lequel mettre à jour l'accroche est nécessaire.");
        }

        // 2. Résolution du produit concerné.
        Product product = this.productProvider.read(command.productId)
                .orElseThrow(() -> new RuntimeException("Le produit " + command.productId.value() + " sur lequel mettre à jour l'accroche n'existe pas."));

        // 3. Modification de l'accroche sur le produit
        product.setPrice(command.price);

        // 4. Enregistrer le produit chez le fournisseur
        return this.productProvider.write(product);
    }

    public record Command(
            ProductId productId,
            BigDecimal price
    ) {
    }
}
