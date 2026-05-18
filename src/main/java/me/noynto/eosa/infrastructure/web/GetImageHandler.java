package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.DownloadImage;
import me.noynto.eosa.shared.ImageId;

public record GetImageHandler(DownloadImage downloadImage) implements Handler {

    @Override
    public void handle(Context ctx) {
        var result = downloadImage.handle(new DownloadImage.Command(new ImageId(ctx.pathParam("id"))));
        if (result.isEmpty()) {
            ctx.status(404);
            return;
        }
        var image = result.get();
        ctx.contentType(image.getFormat());
        ctx.result(image.getContent());
    }

}