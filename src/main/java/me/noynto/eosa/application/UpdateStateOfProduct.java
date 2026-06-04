package me.noynto.eosa.application;

import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.product.ProductState;
import me.noynto.eosa.shared.ProductId;

public record UpdateStateOfProduct(
        ProductProvider productProvider
) {

    public Product handle(Command command) {
        // 1. Vérification
        if (command.state == null) {
            throw new RuntimeException("L'état du produit est nécessaire.");
        }
        if (command.productId == null || command.productId.value() == null) {
            throw new RuntimeException("L'identifiant du produit sur lequel mettre à jour l'accroche est nécessaire.");
        }

        // 2. Résolution du produit concerné.
        Product product = this.productProvider.read(command.productId)
                .orElseThrow(() -> new RuntimeException("Le produit " + command.productId.value() + " sur lequel mettre à jour l'accroche n'existe pas."));

        if (product.getState() == null) {
            product.setState(ProductState.DRAFTED);
        }

        if (product.getState() == command.state) {
            throw new RuntimeException("Le produit " + product.getId().value() + " est déjà dans l'état demandé.");
        }

        if (product.getState() == ProductState.PUBLISHED && command.state == ProductState.DRAFTED) {
            throw new RuntimeException("Un produit publié ne peut pas passer à nouveau brouillon.");
        }

        if (command.state == ProductState.PUBLISHED) {
            if (product.getName() == null || product.getName().isBlank()) {
                throw new RuntimeException("Le produit " + product.getId().value() + " ne peut pas être publié, il lui faut un nom.");
            }
            if (product.getCategory() == null) {
                throw new RuntimeException("Le produit " + product.getId().value() + " ne peut pas être publié, il lui faut une catégorie.");
            }
            if (product.getTagline() == null) {
                throw new RuntimeException("Le produit " + product.getId().value() + " ne peut pas être publié, il lui faut une accroche.");
            }
            if (product.getPrice() == null) {
                throw new RuntimeException("Le produit " + product.getId().value() + " ne peut pas être publié, il lui faut un prix.");
            }
            if (product.getImageIds() == null || product.getImageIds().isEmpty()) {
                throw new RuntimeException("Le produit " + product.getId().value() + " ne peut pas être publié, il lui faut au moins une image.");
            }
        }

        // 3. Modification de l'état sur le produit
        product.setState(command.state);

        // 4. Enregistrer le produit chez le fournisseur
        return this.productProvider.write(product);
    }

    public record Command(
            ProductId productId,
            ProductState state
    ) {
    }
}
