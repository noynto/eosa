package me.noynto.eosa.application;

import me.noynto.eosa.collection.Collection;
import me.noynto.eosa.collection.CollectionProvider;
import me.noynto.eosa.collection.CollectionState;
import me.noynto.eosa.jewel.Jewel;
import me.noynto.eosa.jewel.JewelCategory;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.jewel.JewelState;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.VideoId;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Ce que la page d'accueil lit en une fois : pour chaque catégorie demandée, le nombre de bijoux publiés et le plus bas
 * prix, et le hero, c'est-à-dire la collection publiée la plus récente qui a sa vidéo et son affiche. Le hero est le
 * premier élément de la page : le charger à part, par htmx, le ferait apparaître après coup.
 */
public record ReadHome(
        JewelProvider jewelProvider,
        CollectionProvider collectionProvider
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

        // Les plus récemment publiées d'abord : la première qui a ses médias fait le hero.
        Optional<Hero> hero = collectionProvider.readIds(Set.of(CollectionState.PUBLISHED))
                .map(id -> collectionProvider.read(id).orElseThrow())
                .filter(collection -> collection.getVideoId() != null && collection.getPosterImageId() != null)
                .findFirst()
                .map(ReadHome::toHero);
        return new Result(stats, hero);
    }

    private static Hero toHero(Collection collection) {
        return new Hero(collection.getName(), collection.getVideoId(), collection.getPosterImageId());
    }

    public record Query(
            List<JewelCategory> categories
    ) {
    }

    /**
     * @param stats une entrée par catégorie demandée, dans le même ordre
     * @param hero  vide quand aucune collection publiée n'a de vidéo : l'accueil garde alors son image fixe
     */
    public record Result(
            List<Stats> stats,
            Optional<Hero> hero
    ) {
    }

    public record Stats(
            JewelCategory category,
            int count,
            BigDecimal minPrice
    ) {
    }

    public record Hero(
            String collectionName,
            VideoId videoId,
            ImageId posterImageId
    ) {
    }

}
