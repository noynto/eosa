package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadCategoryStats;
import me.noynto.eosa.application.ReadJewelIds;
import me.noynto.eosa.jewel.JewelCategory;
import me.noynto.eosa.jewel.JewelState;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public record GetIndexHandler(ReadCategoryStats readCategoryStats, ReadJewelIds readJewelIds) implements Handler {

    @Override
    public void handle(Context ctx) {
        var necklaces = readCategoryStats.handle(JewelCategory.NECKLACE);
        var bracelets = readCategoryStats.handle(JewelCategory.BRACELET);
        var latestJewelIds = readJewelIds.handle(new ReadJewelIds.Query(
                Set.of(JewelState.PUBLISHED), Set.of(), 4
        ));

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Eosa — Bijoux faits main à Nancy");
        model.put("necklacesCountLabel", pieceLabel(necklaces.count()));
        model.put("necklacesHasMinPrice", necklaces.minPrice() != null);
        model.put("necklacesMinPrice", necklaces.minPrice() != null ? necklaces.minPrice().stripTrailingZeros().toPlainString() : "");
        model.put("braceletsCountLabel", pieceLabel(bracelets.count()));
        model.put("braceletsHasMinPrice", bracelets.minPrice() != null);
        model.put("braceletsMinPrice", bracelets.minPrice() != null ? bracelets.minPrice().stripTrailingZeros().toPlainString() : "");
        model.put("latestJewelIds", latestJewelIds.stream().map(id -> Map.of("id", id.value())).toList());
        ctx.render("index.mustache", model);
    }

    private static String pieceLabel(int count) {
        return count + " pièce" + (count > 1 ? "s" : "");
    }

}