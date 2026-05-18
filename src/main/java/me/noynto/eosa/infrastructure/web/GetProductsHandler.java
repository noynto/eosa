package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadProductIds;

import java.util.Map;

public record GetProductsHandler(ReadProductIds readProductIds) implements Handler {

    @Override
    public void handle(Context ctx) {
        var productIds = readProductIds.handle();
        ctx.render("products.jte", Map.of("productIds", productIds));
    }

}