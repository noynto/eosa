package me.noynto.eosa.application;

import me.noynto.eosa.collection.Collection;
import me.noynto.eosa.collection.CollectionProvider;
import me.noynto.eosa.jewel.Jewel;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.shared.CollectionId;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Tous les bijoux (quel que soit leur état), chacun marqué selon qu'il est ou non dans la collection : la liste à cocher de la fiche. */
public record ReadCollectionJewelChoices(
        CollectionProvider collectionProvider,
        JewelProvider jewelProvider
) {

    public Result handle(Command command) {
        Collection collection = collectionProvider.read(command.collectionId())
                .orElseThrow(() -> new RuntimeException("La collection " + command.collectionId().value() + " n'existe pas."));
        Set<String> selected = new HashSet<>();
        collection.getJewelIds().forEach(id -> selected.add(id.value()));

        List<Choice> choices = jewelProvider.readIds(null, null)
                .map(id -> jewelProvider.read(id).orElseThrow())
                .map(jewel -> new Choice(jewel, selected.contains(jewel.getId().value())))
                .toList();
        return new Result(collection, choices);
    }

    public record Choice(
            Jewel jewel,
            boolean selected
    ) {
    }

    public record Result(
            Collection collection,
            List<Choice> choices
    ) {
    }

    public record Command(
            CollectionId collectionId
    ) {
    }

}
