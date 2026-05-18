package me.noynto.eosa.application;

import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.shared.ProductId;

import java.util.List;

public record ReadProductIds(
        ProductProvider productProvider
) {

    public List<ProductId> handle() {
        return productProvider.readIds().toList();
    }

}