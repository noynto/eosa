package me.noynto.eosa.application;

import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.product.ProductState;
import me.noynto.eosa.product.Variant;
import me.noynto.eosa.shared.OptionId;
import me.noynto.eosa.shared.OptionValueId;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;

public record ReadOptionValueStats(
        ProductProvider productProvider,
        ReadProductIds readProductIds
) {

    public Stats handle(OptionId optionId, OptionValueId optionValueId) {
        var ids = readProductIds.handle(new ReadProductIds.Query(
                Set.of(ProductState.PUBLISHED),
                optionId,
                optionValueId
        ));
        int count = ids.size();
        BigDecimal minPrice = ids.stream()
                .map(id -> productProvider.read(id).orElseThrow())
                .flatMap(product -> product.getVariants().stream())
                .filter(variant -> optionValueId.equals(variant.getOptionValues().get(optionId)))
                .map(Variant::getPrice)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(null);
        return new Stats(optionId, optionValueId, count, minPrice);
    }

    public record Stats(
            OptionId optionId,
            OptionValueId optionValueId,
            int count,
            BigDecimal minPrice
    ) {
    }

}
