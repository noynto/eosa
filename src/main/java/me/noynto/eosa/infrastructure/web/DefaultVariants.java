package me.noynto.eosa.infrastructure.web;

import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.Variant;

final class DefaultVariants {

    private DefaultVariants() {
    }

    static Variant resolve(Product product) {
        if (product.getDefaultVariantId() == null) {
            throw new RuntimeException("Le produit " + product.getId().value() + " n'a pas de variant par défaut.");
        }
        return product.getVariants().stream()
                .filter(v -> v.getId().equals(product.getDefaultVariantId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Le variant par défaut du produit " + product.getId().value() + " est introuvable."));
    }

}
