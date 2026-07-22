package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadProduct;
import me.noynto.eosa.application.ReadProductIds;
import me.noynto.eosa.product.ProductState;
import me.noynto.eosa.shared.ProductId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

public record GetProductHandler(ReadProduct readProduct, ReadProductIds readProductIds) implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        try {
            var productId = new ProductId(ctx.pathParam("id"));
            var product = readProduct.handle(new ReadProduct.Command(productId));
            var defaultVariant = product.getVariants().stream()
                    .filter(v -> v.getId().equals(product.getDefaultVariantId()))
                    .findFirst()
                    .orElse(null);
            var allIds = new ArrayList<>(readProductIds.handle(new ReadProductIds.Query(Set.of(ProductState.PUBLISHED))));
            allIds.remove(productId);
            Collections.shuffle(allIds);
            var relatedIds = allIds.stream().limit(4).toList();

            var model = new HashMap<String, Object>();
            model.put("product", product);
            model.put("defaultVariant", defaultVariant);
            model.put("relatedIds", relatedIds);
            ctx.render("product.jte", model);
        } catch (RuntimeException e) {
            ctx.status(404);
        }
    }

}
