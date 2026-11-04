package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.DownloadImage;
import me.noynto.eosa.image.Image;
import me.noynto.eosa.shared.ImageId;

public record GetImageHandler(DownloadImage downloadImage) implements Handler {

    @Override
    public void handle(Context ctx) {
        Image image;
        try {
            image = downloadImage.handle(new DownloadImage.Command(new ImageId(ctx.pathParam("id")))).image();
        } catch (RuntimeException e) {
            ctx.status(404);
            return;
        }
        ctx.contentType(image.getFormat());
        // Ouverte seule dans un onglet, une image ne peut exécuter aucun script, même si son format était mal déclaré.
        ctx.header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; sandbox");
        ctx.result(image.getContent());
    }

}