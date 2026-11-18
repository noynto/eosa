package me.noynto.eosa.application;

import me.noynto.eosa.collection.Collection;
import me.noynto.eosa.collection.CollectionProvider;
import me.noynto.eosa.image.Image;
import me.noynto.eosa.image.ImageProvider;
import me.noynto.eosa.shared.CollectionId;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.VideoId;
import me.noynto.eosa.video.Video;
import me.noynto.eosa.video.VideoProvider;

/**
 * Envoie la vidéo et/ou l'affiche de la collection. Chacune remplace la précédente, qui est supprimée une fois la
 * collection enregistrée : une erreur en cours de route ne laisse jamais une collection sans média.
 */
public record UploadCollectionMedia(
        CollectionProvider collectionProvider,
        VideoProvider videoProvider,
        ImageProvider imageProvider
) {

    public Result handle(Command command) {
        if (command.collectionId == null || command.collectionId.value() == null) {
            throw new InvalidCommand("L'identifiant de la collection est requis.");
        }
        if (command.video == null && command.poster == null) {
            throw new InvalidCommand("Choisissez une vidéo ou une affiche à envoyer.");
        }
        if (command.video != null) {
            if (!Video.ACCEPTED_FORMATS.contains(command.video.getFormat())) {
                throw new InvalidCommand("Le format de la vidéo n'est pas accepté : MP4.");
            }
            if (command.video.getContent() == null || command.video.getContent().length == 0) {
                throw new InvalidCommand("La vidéo est vide.");
            }
            if (command.video.getContent().length > Video.MAX_SIZE) {
                throw new InvalidCommand("La vidéo dépasse " + Video.MAX_SIZE / (1024 * 1024) + " Mo.");
            }
        }
        if (command.poster != null && !Image.ACCEPTED_FORMATS.contains(command.poster.getFormat())) {
            throw new InvalidCommand("Le format de l'affiche n'est pas accepté : JPEG, PNG, WebP ou AVIF.");
        }

        Collection collection = collectionProvider.read(command.collectionId)
                .orElseThrow(() -> new InvalidCommand("La collection " + command.collectionId.value() + " n'existe pas."));

        VideoId previousVideoId = collection.getVideoId();
        ImageId previousPosterId = collection.getPosterImageId();
        if (command.video != null) {
            collection.setVideoId(videoProvider.write(command.video).getId());
        }
        if (command.poster != null) {
            collection.setPosterImageId(imageProvider.upload(command.poster).getId());
        }
        Collection saved = collectionProvider.write(collection);

        if (command.video != null && previousVideoId != null) {
            videoProvider.delete(previousVideoId);
        }
        if (command.poster != null && previousPosterId != null) {
            imageProvider.delete(previousPosterId);
        }
        return new Result(saved);
    }

    public record Result(
            Collection collection
    ) {
    }

    /** {@code video} et {@code poster} sont nuls quand on n'en envoie pas. */
    public record Command(
            CollectionId collectionId,
            Video video,
            Image poster
    ) {
    }

    public static class InvalidCommand extends RuntimeException {
        public InvalidCommand(String message) {
            super(message);
        }
    }

}
