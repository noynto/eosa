package me.noynto.eosa.application;

import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductCategory;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.product.ProductState;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;

public record ReadCategoryStats(
        ProductProvider productProvider,
        ReadProductIds readProductIds
) {

    public Stats handle(ProductCategory category) {
        var ids = readProductIds.handle(new ReadProductIds.Query(
                Set.of(ProductState.PUBLISHED),
                Set.of(category)
        ));
        int count = ids.size();
        BigDecimal minPrice = ids.stream()
                .map(id -> productProvider.read(id).orElseThrow())
                .map(Product::getPrice)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(null);
        return new Stats(category, count, minPrice);
    }

    public record Stats(
            ProductCategory category,
            int count,
            BigDecimal minPrice
    ) {
    }

}