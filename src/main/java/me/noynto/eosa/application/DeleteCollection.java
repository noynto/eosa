package me.noynto.eosa.application;

import me.noynto.eosa.collection.Collection;
import me.noynto.eosa.collection.CollectionProvider;
import me.noynto.eosa.collection.CollectionState;
import me.noynto.eosa.image.ImageProvider;
import me.noynto.eosa.shared.CollectionId;
import me.noynto.eosa.video.VideoProvider;

/** Supprime la collection, sa vidéo et son affiche ; ses bijoux restent, avec leur état. Une collection publiée alimente l'accueil : on l'archive d'abord. */
public record DeleteCollection(
        CollectionProvider collectionProvider,
        VideoProvider videoProvider,
        ImageProvider imageProvider
) {

    public void handle(Command command) {
        if (command.collectionId == null || command.collectionId.value() == null) {
            throw new InvalidCommand("L'identifiant de la collection est requis.");
        }
        Collection collection = collectionProvider.read(command.collectionId)
                .orElseThrow(() -> new InvalidCommand("La collection " + command.collectionId.value() + " n'existe pas."));
        if (collection.getState() == CollectionState.PUBLISHED) {
            throw new InvalidCommand("Une collection publiée ne peut pas être supprimée : archivez-la d'abord.");
        }

        collectionProvider.delete(command.collectionId);
        if (collection.getVideoId() != null) {
            videoProvider.delete(collection.getVideoId());
        }
        if (collection.getPosterImageId() != null) {
            imageProvider.delete(collection.getPosterImageId());
        }
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
