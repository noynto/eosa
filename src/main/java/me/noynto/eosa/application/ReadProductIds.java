package me.noynto.eosa.application;

import me.noynto.eosa.product.ProductCategory;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.product.ProductState;
import me.noynto.eosa.shared.ProductId;

import java.util.List;
import java.util.Set;

public record ReadProductIds(
        ProductProvider productProvider
) {

    public List<ProductId> handle(Query query) {
        return productProvider.readIds(query.states, query.categories).toList();
    }

    public record Query(
            Set<ProductState> states,
            Set<ProductCategory> categories
    ) {
    }

}