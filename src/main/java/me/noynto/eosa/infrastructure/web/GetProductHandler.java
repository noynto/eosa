package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadProduct;
import me.noynto.eosa.application.ReadProductIds;
import me.noynto.eosa.product.ProductState;
import me.noynto.eosa.shared.ProductId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public record GetProductHandler(ReadProduct readProduct, ReadProductIds readProductIds) implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        try {
            var productId = new ProductId(ctx.pathParam("id"));
            var product = readProduct.handle(new ReadProduct.Command(productId));
            var allIds = new ArrayList<>(readProductIds.handle(new ReadProductIds.Query(Set.of(ProductState.PUBLISHED), Set.of())));
            allIds.remove(productId);
            Collections.shuffle(allIds);
            var relatedIds = allIds.stream().limit(4).toList();
            ctx.render("product.jte", Map.of("product", product, "relatedIds", relatedIds));
        } catch (RuntimeException e) {
            ctx.status(404);
        }
    }

}