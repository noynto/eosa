package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadCategoryStats;
import me.noynto.eosa.product.ProductCategory;

import java.util.Map;

public record GetIndexHandler(ReadCategoryStats readCategoryStats) implements Handler {

    @Override
    public void handle(Context ctx) {
        var necklaces = readCategoryStats.handle(ProductCategory.NECKLACE);
        var bracelets = readCategoryStats.handle(ProductCategory.BRACELET);
        ctx.render("index.jte", Map.of("necklaces", necklaces, "bracelets", bracelets));
    }

}