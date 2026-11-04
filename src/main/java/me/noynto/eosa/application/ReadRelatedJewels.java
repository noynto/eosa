package me.noynto.eosa.application;

import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.jewel.JewelState;
import me.noynto.eosa.shared.JewelId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Des bijoux publiés à proposer sous la fiche d'un bijou : tirés au hasard, jamais le bijou lui-même.
 */
public record ReadRelatedJewels(
        JewelProvider jewelProvider
) {

    public Result handle(Query query) {
        if (query.jewelId == null || query.jewelId.value() == null) {
            throw new RuntimeException("L'identifiant du produit est nécessaire.");
        }
        if (query.limit < 1) {
            throw new RuntimeException("Au moins un bijou doit être demandé.");
        }
        List<JewelId> jewelIds = new ArrayList<>(jewelProvider.readIds(Set.of(JewelState.PUBLISHED), Set.of()).toList());
        jewelIds.remove(query.jewelId);
        Collections.shuffle(jewelIds);
        return new Result(jewelIds.stream().limit(query.limit).toList());
    }

    public record Query(
            JewelId jewelId,
            int limit
    ) {
    }

    public record Result(
            List<JewelId> jewelIds
    ) {
    }

}
