package me.noynto.eosa.application;

import me.noynto.eosa.collection.Collection;
import me.noynto.eosa.collection.CollectionProvider;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.shared.CollectionId;
import me.noynto.eosa.shared.JewelId;

import java.util.List;

/** Remplace la liste des bijoux de la collection ; ne change ni l'état de la collection ni celui des bijoux. */
public record UpdateJewelsOfCollection(
        CollectionProvider collectionProvider,
        JewelProvider jewelProvider
) {

    public Result handle(Command command) {
        if (command.collectionId == null || command.collectionId.value() == null) {
            throw new InvalidCommand("L'identifiant de la collection est requis.");
        }
        Collection collection = collectionProvider.read(command.collectionId)
                .orElseThrow(() -> new InvalidCommand("La collection " + command.collectionId.value() + " n'existe pas."));

        List<JewelId> jewelIds = command.jewelIds == null ? List.of() : command.jewelIds.stream().distinct().toList();
        for (JewelId jewelId : jewelIds) {
            if (jewelProvider.read(jewelId).isEmpty()) {
                throw new InvalidCommand("Le bijou " + jewelId.value() + " n'existe pas.");
            }
        }

        collection.setJewelIds(jewelIds);
        return new Result(collectionProvider.write(collection));
    }

    public record Result(
            Collection collection
    ) {
    }

    public record Command(
            CollectionId collectionId,
            List<JewelId> jewelIds
    ) {
    }

    public static class InvalidCommand extends RuntimeException {
        public InvalidCommand(String message) {
            super(message);
        }
    }

}
