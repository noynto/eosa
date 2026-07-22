package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadProductIds;

import java.util.Map;
import java.util.Set;

public record GetAdminProductsHandler(ReadProductIds readProductIds) implements Handler {

    @Override
    public void handle(Context ctx) {
        var ids = readProductIds.handle(new ReadProductIds.Query(Set.of()));
        ctx.render("admin/products.jte", Map.of("productIds", ids));
    }

}