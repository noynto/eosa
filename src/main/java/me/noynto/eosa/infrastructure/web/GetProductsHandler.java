package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadProductIds;
import me.noynto.eosa.product.ProductState;

import java.util.Map;
import java.util.Set;

public record GetProductsHandler(
        ReadProductIds readProductIds,
        String title
) implements Handler {

    @Override
    public void handle(Context ctx) {
        var productIds = readProductIds.handle(new ReadProductIds.Query(Set.of(ProductState.PUBLISHED)));
        ctx.render("products.jte", Map.of("productIds", productIds, "title", title));
    }

}
