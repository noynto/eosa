package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadProductIds;
import me.noynto.eosa.product.ProductState;

import java.util.Map;
import java.util.Set;

public record GetIndexHandler(ReadProductIds readProductIds) implements Handler {

    @Override
    public void handle(Context ctx) {
        var latestProductIds = readProductIds.handle(new ReadProductIds.Query(Set.of(ProductState.PUBLISHED)))
                .stream()
                .limit(4)
                .toList();
        ctx.render("index.jte", Map.of("latestProductIds", latestProductIds));
    }

}
