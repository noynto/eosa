package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.AddImageToMetalColor;
import me.noynto.eosa.image.Image;
import me.noynto.eosa.shared.MetalColorId;

public record AddImageToMetalColorHandler(AddImageToMetalColor addImageToMetalColor) implements Handler {

    @Override
    public void handle(Context ctx) {
        var uploadedFile = ctx.uploadedFile("image");
        try {
            if (uploadedFile == null) {
                throw new RuntimeException("Une image est nécessaire.");
            }
            Image image = new Image();
            image.setName(uploadedFile.filename());
            image.setFormat(uploadedFile.contentType());
            image.setContent(uploadedFile.content());
            addImageToMetalColor.handle(new AddImageToMetalColor.Command(
                    new MetalColorId(ctx.pathParam("id")),
                    image
            ));
            ctx.header("HX-Redirect", "/admin/metal-colors");
            ctx.status(200);
        } catch (RuntimeException e) {
            ctx.status(422).html("<span class=\"text-red-600 text-xs\">" + e.getMessage() + "</span>");
        }
    }

}
