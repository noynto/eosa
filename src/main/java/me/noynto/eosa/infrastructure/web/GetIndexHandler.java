package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadCategoryStats;
import me.noynto.eosa.application.ReadProductIds;
import me.noynto.eosa.product.ProductCategory;
import me.noynto.eosa.product.ProductState;

import java.util.HashMap;
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

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Eosa — Bijoux faits main à Nancy");
        model.put("necklacesCountLabel", pieceLabel(necklaces.count()));
        model.put("necklacesHasMinPrice", necklaces.minPrice() != null);
        model.put("necklacesMinPrice", necklaces.minPrice() != null ? necklaces.minPrice().stripTrailingZeros().toPlainString() : "");
        model.put("braceletsCountLabel", pieceLabel(bracelets.count()));
        model.put("braceletsHasMinPrice", bracelets.minPrice() != null);
        model.put("braceletsMinPrice", bracelets.minPrice() != null ? bracelets.minPrice().stripTrailingZeros().toPlainString() : "");
        model.put("latestProductIds", latestProductIds.stream().map(id -> Map.of("id", id.value())).toList());
        ctx.render("index.mustache", model);
    }

    private static String pieceLabel(int count) {
        return count + " pièce" + (count > 1 ? "s" : "");
    }

}