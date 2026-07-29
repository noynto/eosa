package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadProduct;
import me.noynto.eosa.shared.ProductId;

import java.util.HashMap;
import java.util.Map;

public record GetProductCardHandler(ReadProduct readProduct) implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        try {
            var product = readProduct.handle(new ReadProduct.Command(new ProductId(ctx.pathParam("id"))));
            boolean hasImage = !product.getImageIds().isEmpty();
            boolean hasTagline = product.getTagline() != null;
            Map<String, Object> model = new HashMap<>();
            model.put("productId", product.getId().value());
            model.put("hasImage", hasImage);
            model.put("mainImageId", hasImage ? product.getImageIds().getFirst().value() : "");
            model.put("name", product.getName());
            model.put("price", product.getPrice().stripTrailingZeros().toPlainString());
            model.put("hasTagline", hasTagline);
            model.put("tagline", hasTagline ? product.getTagline() : "");
            ctx.render("partials/product-card.mustache", model);
        } catch (RuntimeException e) {
            ctx.status(404);
        }
    }

}