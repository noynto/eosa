package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadCategoryStats;
import me.noynto.eosa.jewel.JewelCategory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record GetIndexHandler(ReadCategoryStats readCategoryStats, String baseUrl) implements Handler {

    @Override
    public void handle(Context ctx) {
        var stats = readCategoryStats.handle(new ReadCategoryStats.Query(
                List.of(JewelCategory.NECKLACE, JewelCategory.BRACELET)
        )).stats();
        var necklaces = stats.get(0);
        var bracelets = stats.get(1);

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Eosa — Bijoux faits main à Nancy");
        model.put("description", "Bijoux faits main à Nancy — colliers, bracelets et pierres en matières nobles, conçus pour durer.");
        model.put("ogImageUrl", baseUrl + "/hero.webp");
        model.put("canonicalUrl", baseUrl + ctx.path());
        model.put("jsonLd", JsonLd.of(Map.of(
                "@context", "https://schema.org",
                "@graph", List.of(
                        JsonLd.organization(baseUrl),
                        Map.of(
                                "@type", "WebSite",
                                "name", "EOSA",
                                "url", baseUrl + "/",
                                "inLanguage", "fr-FR"
                        )
                )
        )));
        model.put("necklacesCountLabel", pieceLabel(necklaces.count()));
        model.put("necklacesHasMinPrice", necklaces.minPrice() != null);
        model.put("necklacesMinPrice", necklaces.minPrice() != null ? necklaces.minPrice().stripTrailingZeros().toPlainString() : "");
        model.put("braceletsCountLabel", pieceLabel(bracelets.count()));
        model.put("braceletsHasMinPrice", bracelets.minPrice() != null);
        model.put("braceletsMinPrice", bracelets.minPrice() != null ? bracelets.minPrice().stripTrailingZeros().toPlainString() : "");
        ctx.render("index.mustache", model);
    }

    private static String pieceLabel(int count) {
        return count + " pièce" + (count > 1 ? "s" : "");
    }

}