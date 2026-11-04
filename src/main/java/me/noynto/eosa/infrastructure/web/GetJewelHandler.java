package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadJewel;
import me.noynto.eosa.shared.JewelId;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fiche d'un bijou : tout ce qui sert à la lire et à la référencer. Les options de personnalisation et les autres
 * bijoux se chargent à part (voir {@link GetMetalColorOptionsHandler}, {@link GetCharmOptionsHandler} et
 * {@link GetRelatedJewelsHandler}).
 */
public record GetJewelHandler(ReadJewel readJewel, String baseUrl) implements Handler {

    private static final String THUMB_ACTIVE = "w-16 h-16 bg-bg-soft rounded-xl border-2 border-primary relative overflow-hidden flex-shrink-0 cursor-pointer";
    private static final String THUMB_INACTIVE = "w-16 h-16 bg-bg-soft rounded-xl relative overflow-hidden flex-shrink-0 cursor-pointer";

    @Override
    public void handle(Context ctx) throws Exception {
        try {
            var jewelId = new JewelId(ctx.pathParam("id"));
            var jewel = readJewel.handle(new ReadJewel.Command(jewelId)).jewel();

            boolean hasImages = !jewel.getImageIds().isEmpty();
            List<Map<String, Object>> images = new ArrayList<>();
            for (int i = 0; i < jewel.getImageIds().size(); i++) {
                Map<String, Object> image = new HashMap<>();
                image.put("id", jewel.getImageIds().get(i).value());
                image.put("thumbClass", i == 0 ? THUMB_ACTIVE : THUMB_INACTIVE);
                images.add(image);
            }

            Map<String, Object> model = new HashMap<>();
            model.put("title", "Eosa — " + jewel.getName());
            model.put("name", jewel.getName());
            model.put("tagline", jewel.getTagline() != null ? jewel.getTagline() : "");
            model.put("description", jewel.getTagline() != null ? jewel.getTagline() : "Bijoux faits main à Nancy — colliers, bracelets et pierres en matières nobles, conçus pour durer.");
            model.put("ogImageUrl", hasImages ? baseUrl + "/images/" + jewel.getImageIds().getFirst().value() : baseUrl + "/hero.webp");
            model.put("canonicalUrl", baseUrl + ctx.path());
            model.put("price", jewel.getPrice().stripTrailingZeros().toPlainString());
            model.put("jsonLd", JsonLd.of(Map.of(
                    "@context", "https://schema.org",
                    "@type", "Product",
                    "name", jewel.getName(),
                    "description", model.get("description"),
                    "image", hasImages
                            ? jewel.getImageIds().stream().map(id -> baseUrl + "/images/" + id.value()).toList()
                            : List.of(baseUrl + "/hero.webp"),
                    "brand", Map.of("@type", "Brand", "name", "EOSA"),
                    "offers", Map.of(
                            "@type", "Offer",
                            "url", baseUrl + ctx.path(),
                            "priceCurrency", "EUR",
                            "price", jewel.getPrice().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                            "itemCondition", "https://schema.org/NewCondition",
                            "availability", "https://schema.org/InStock",
                            "seller", JsonLd.organization(baseUrl)
                    )
            )));
            model.put("jewelId", jewel.getId().value());
            model.put("hasImages", hasImages);
            model.put("mainImageId", hasImages ? jewel.getImageIds().getFirst().value() : "");
            model.put("images", images);

            ctx.render("jewel.mustache", model);
        } catch (RuntimeException e) {
            ctx.status(404);
        }
    }

}