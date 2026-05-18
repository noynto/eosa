package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.AddImagesToProduct;
import me.noynto.eosa.image.Image;
import me.noynto.eosa.shared.ProductId;

public record AddImagesToProductHandler(
        AddImagesToProduct addImagesToProduct,
        String adminId,
        String adminSecret
) implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        if (!BasicAuth.isAuthorized(ctx.header("Authorization"), adminId, adminSecret)) {
            ctx.status(401);
            return;
        }
        var images = ctx.uploadedFiles("images").stream().map(file -> {
            Image image = new Image();
            image.setName(file.filename());
            image.setFormat(file.contentType());
            image.setContent(file.content());
            return image;
        }).toList();
        addImagesToProduct.handle(new AddImagesToProduct.Command(
                new ProductId(ctx.pathParam("id")),
                images
        ));
        ctx.status(204);
    }

}