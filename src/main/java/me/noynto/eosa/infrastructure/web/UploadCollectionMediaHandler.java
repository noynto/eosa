package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.UploadedFile;
import me.noynto.eosa.application.UploadCollectionMedia;
import me.noynto.eosa.image.Image;
import me.noynto.eosa.shared.CollectionId;
import me.noynto.eosa.video.Video;

import java.io.IOException;
import java.io.UncheckedIOException;

public record UploadCollectionMediaHandler(UploadCollectionMedia uploadCollectionMedia) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            uploadCollectionMedia.handle(new UploadCollectionMedia.Command(
                    new CollectionId(ctx.pathParam("id")),
                    toVideo(ctx.uploadedFile("video")),
                    toPoster(ctx.uploadedFile("poster"))
            ));
            ctx.header("HX-Redirect", "/collections/" + ctx.pathParam("id"));
            ctx.status(200);
        } catch (RuntimeException e) {
            InlineError.render(ctx, e);
        }
    }

    /** Un champ de fichier laissé vide arrive avec un nom vide : rien n'est envoyé. */
    private static Video toVideo(UploadedFile file) {
        if (file == null || file.filename().isBlank()) {
            return null;
        }
        Video video = new Video();
        video.setName(file.filename());
        video.setFormat(file.contentType());
        try {
            // Un octet de plus que la limite suffit pour la refuser, sans lire un fichier énorme en mémoire.
            video.setContent(file.content().readNBytes(Video.MAX_SIZE + 1));
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de lire la vidéo.", e);
        }
        return video;
    }

    private static Image toPoster(UploadedFile file) {
        if (file == null || file.filename().isBlank()) {
            return null;
        }
        Image image = new Image();
        image.setName(file.filename());
        image.setFormat(file.contentType());
        image.setContent(file.content());
        return image;
    }

}
