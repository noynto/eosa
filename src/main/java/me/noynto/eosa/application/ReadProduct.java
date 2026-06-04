package me.noynto.eosa.application;

import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.shared.ProductId;

public record ReadProduct(
        ProductProvider productProvider
) {

    public Product handle(Command command) {
        // 1. Vérification de la commande.
        if (command.productId == null || command.productId.value() == null) {
            throw new RuntimeException("L'identifiant du produit sur lequel mettre à jour l'accroche est nécessaire.");
        }
        // 2. Résolution du produit concerné.
        return productProvider.read(command.productId())
                .orElseThrow(() -> new RuntimeException("Le produit " + command.productId.value() + " n'existe pas."));
    }

    public record Command(
            ProductId productId
    ) {
    }

}
