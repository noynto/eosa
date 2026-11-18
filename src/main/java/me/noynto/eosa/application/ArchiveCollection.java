package me.noynto.eosa.application;

import me.noynto.eosa.collection.Collection;
import me.noynto.eosa.collection.CollectionProvider;
import me.noynto.eosa.collection.CollectionState;
import me.noynto.eosa.shared.CollectionId;

/** Archive une collection publiée : sa vidéo quitte l'accueil, ses bijoux restent en vente. */
public record ArchiveCollection(
        CollectionProvider collectionProvider
) {

    public Result handle(Command command) {
        if (command.collectionId == null || command.collectionId.value() == null) {
            throw new InvalidCommand("L'identifiant de la collection est requis.");
        }
        Collection collection = collectionProvider.read(command.collectionId)
                .orElseThrow(() -> new InvalidCommand("La collection " + command.collectionId.value() + " n'existe pas."));
        if (collection.getState() != CollectionState.PUBLISHED) {
            throw new InvalidCommand("Seule une collection publiée peut être archivée.");
        }

        collection.setState(CollectionState.ARCHIVED);
        return new Result(collectionProvider.write(collection));
    }

    public record Result(
            Collection collection
    ) {
    }

    public record Command(
            CollectionId collectionId
    ) {
    }

    public static class InvalidCommand extends RuntimeException {
        public InvalidCommand(String message) {
            super(message);
        }
    }

}
