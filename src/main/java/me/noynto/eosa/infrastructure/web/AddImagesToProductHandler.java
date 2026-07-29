package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.AddImagesToProduct;
import me.noynto.eosa.image.Image;
import me.noynto.eosa.product.Product;
import me.noynto.eosa.shared.ProductId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record AddImagesToProductHandler(AddImagesToProduct addImagesToProduct) implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        var images = ctx.uploadedFiles("images").stream().map(file -> {
            Image image = new Image();
            image.setName(file.filename());
            image.setFormat(file.contentType());
            image.setContent(file.content());
            return image;
        }).toList();
        try {
            Product product = addImagesToProduct.handle(new AddImagesToProduct.Command(
                    new ProductId(ctx.pathParam("id")),
                    images
            ));
            Map<String, Object> model = new HashMap<>();
            model.put("productId", product.getId().value());
            model.put("hasImages", !product.getImageIds().isEmpty());
            model.put("images", product.getImageIds().stream().map(id -> Map.of("id", id.value())).toList());
            ctx.render("admin/partials/product-images.mustache", model);
        } catch (RuntimeException e) {
            ctx.status(422).html("<span class=\"text-red-600 text-xs\">" + e.getMessage() + "</span>");
        }
    }

}