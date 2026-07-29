package me.noynto.eosa.application;

import me.noynto.eosa.jewel.Jewel;
import me.noynto.eosa.jewel.JewelCategory;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.jewel.JewelState;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;

public record ReadCategoryStats(
        JewelProvider jewelProvider,
        ReadJewelIds readJewelIds
) {

    public Stats handle(JewelCategory category) {
        var ids = readJewelIds.handle(new ReadJewelIds.Query(
                Set.of(JewelState.PUBLISHED),
                Set.of(category)
        ));
        int count = ids.size();
        BigDecimal minPrice = ids.stream()
                .map(id -> jewelProvider.read(id).orElseThrow())
                .map(Jewel::getPrice)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(null);
        return new Stats(category, count, minPrice);
    }

    public record Stats(
            JewelCategory category,
            int count,
            BigDecimal minPrice
    ) {
    }

}