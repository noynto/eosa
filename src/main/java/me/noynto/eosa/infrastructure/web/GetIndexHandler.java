package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadCategoryStats;
import me.noynto.eosa.application.ReadProductIds;
import me.noynto.eosa.product.ProductCategory;
import me.noynto.eosa.product.ProductState;

import java.util.Map;
import java.util.Set;

public record GetIndexHandler(ReadCategoryStats readCategoryStats, ReadProductIds readProductIds) implements Handler {

    @Override
    public void handle(Context ctx) {
        var necklaces = readCategoryStats.handle(ProductCategory.NECKLACE);
        var bracelets = readCategoryStats.handle(ProductCategory.BRACELET);
        var latestProductIds = readProductIds.handle(new ReadProductIds.Query(
                Set.of(ProductState.PUBLISHED), Set.of(), 4
        ));
        ctx.render("index.jte", Map.of("necklaces", necklaces, "bracelets", bracelets, "latestProductIds", latestProductIds));
    }

}