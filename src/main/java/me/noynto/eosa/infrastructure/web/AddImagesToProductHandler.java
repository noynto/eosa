package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.AddImagesToVariant;
import me.noynto.eosa.application.ReadProduct;
import me.noynto.eosa.image.Image;
import me.noynto.eosa.product.Product;
import me.noynto.eosa.shared.ProductId;

import java.util.Map;

public record AddImagesToProductHandler(
        ReadProduct readProduct,
        AddImagesToVariant addImagesToVariant
) implements Handler {

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
            var productId = new ProductId(ctx.pathParam("id"));
            Product product = readProduct.handle(new ReadProduct.Command(productId));
            var variant = DefaultVariants.resolve(product);

            Product updated = addImagesToVariant.handle(new AddImagesToVariant.Command(productId, variant.getId(), images));
            var updatedVariant = DefaultVariants.resolve(updated);

            ctx.render("admin/partials/product-images.jte", Map.of("product", updated, "variant", updatedVariant));
        } catch (RuntimeException e) {
            ctx.status(422).html("<span class=\"text-red-600 text-xs\">" + e.getMessage() + "</span>");
        }
    }

}
