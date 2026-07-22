package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadProduct;
import me.noynto.eosa.shared.ProductId;

import java.util.HashMap;

public record GetProductCardHandler(ReadProduct readProduct) implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        try {
            var product = readProduct.handle(new ReadProduct.Command(new ProductId(ctx.pathParam("id"))));
            var defaultVariant = product.getVariants().stream()
                    .filter(v -> v.getId().equals(product.getDefaultVariantId()))
                    .findFirst()
                    .orElse(null);

            var model = new HashMap<String, Object>();
            model.put("product", product);
            model.put("defaultVariant", defaultVariant);
            ctx.render("partials/product-card.jte", model);
        } catch (RuntimeException e) {
            ctx.status(404);
        }
    }

}
