package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadProductIds;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public record GetAdminProductsHandler(ReadProductIds readProductIds) implements Handler {

    @Override
    public void handle(Context ctx) {
        var ids = readProductIds.handle(new ReadProductIds.Query(Set.of(), Set.of()));
        Map<String, Object> model = new HashMap<>();
        model.put("title", "Produits");
        model.put("productIds", ids.stream().map(id -> Map.of("id", id.value())).toList());
        ctx.render("admin/products.mustache", model);
    }

}