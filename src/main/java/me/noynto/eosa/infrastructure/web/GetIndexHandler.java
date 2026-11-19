package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadHome;
import me.noynto.eosa.jewel.JewelCategory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record GetIndexHandler(ReadHome readHome, String baseUrl) implements Handler {

    @Override
    public void handle(Context ctx) {
        var home = readHome.handle(new ReadHome.Query(
                List.of(JewelCategory.NECKLACE, JewelCategory.BRACELET)
        ));
        var stats = home.stats();
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
        model.put("necklacesMinPrice", necklaces.minPrice() != null ? PriceFormat.price(necklaces.minPrice()) : "");
        model.put("braceletsCountLabel", pieceLabel(bracelets.count()));
        model.put("braceletsHasMinPrice", bracelets.minPrice() != null);
        model.put("braceletsMinPrice", bracelets.minPrice() != null ? PriceFormat.price(bracelets.minPrice()) : "");
        // Sans collection publiée qui ait sa vidéo, le hero garde l'image fixe.
        model.put("hasHeroVideo", home.hero().isPresent());
        model.put("heroLabel", "Nouvelle collection");
        model.put("hasHeroHeadline", false);
        model.put("hasHeroBlurb", false);
        home.hero().ifPresent(hero -> {
            model.put("heroCollectionName", hero.collectionName());
            model.put("heroVideoId", hero.videoId().value());
            model.put("heroPosterId", hero.posterImageId().value());
            model.put("heroLabel", "Collection " + hero.collectionName());
            // Sans texte propre à la collection, le hero garde le titre et le paragraphe par défaut.
            model.put("hasHeroHeadline", hero.headline() != null);
            model.put("heroHeadline", hero.headline());
            model.put("hasHeroBlurb", hero.blurb() != null);
            model.put("heroBlurb", hero.blurb());
        });
        ctx.render("index.mustache", model);
    }

    private static String pieceLabel(int count) {
        if (count == 0) {
            return "Bientôt";
        }
        return count + " pièce" + (count > 1 ? "s" : "");
    }

}