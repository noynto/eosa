package me.noynto.eosa.application;

import me.noynto.eosa.jewel.Jewel;
import me.noynto.eosa.jewel.JewelCategory;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.jewel.JewelState;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Pour chaque catégorie demandée : le nombre de bijoux publiés et le plus bas prix.
 */
public record ReadCategoryStats(
        JewelProvider jewelProvider
) {

    public Result handle(Query query) {
        List<Stats> stats = query.categories().stream()
                .map(category -> {
                    List<Jewel> jewels = jewelProvider.readIds(Set.of(JewelState.PUBLISHED), Set.of(category))
                            .map(id -> jewelProvider.read(id).orElseThrow())
                            .toList();
                    BigDecimal minPrice = jewels.stream()
                            .map(Jewel::getPrice)
                            .filter(Objects::nonNull)
                            .min(BigDecimal::compareTo)
                            .orElse(null);
                    return new Stats(category, jewels.size(), minPrice);
                })
                .toList();
        return new Result(stats);
    }

    public record Query(
            List<JewelCategory> categories
    ) {
    }

    /**
     * @param stats une entrée par catégorie demandée, dans le même ordre
     */
    public record Result(
            List<Stats> stats
    ) {
    }

    public record Stats(
            JewelCategory category,
            int count,
            BigDecimal minPrice
    ) {
    }

}
