package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadProductIds;
import me.noynto.eosa.product.ProductCategory;
import me.noynto.eosa.product.ProductState;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public record GetProductsHandler(
        ReadProductIds readProductIds,
        Set<ProductCategory> categories,
        String title
) implements Handler {

    @Override
    public void handle(Context ctx) {
        var productIds = readProductIds.handle(new ReadProductIds.Query(Set.of(ProductState.PUBLISHED), categories));
        Map<String, Object> model = new HashMap<>();
        model.put("title", "Eosa — " + title);
        model.put("heading", title);
        model.put("countLabel", productIds.size() + " pièce" + (productIds.size() > 1 ? "s" : ""));
        model.put("productIds", productIds.stream().map(id -> Map.of("id", id.value())).toList());
        ctx.render("products.mustache", model);
    }

}