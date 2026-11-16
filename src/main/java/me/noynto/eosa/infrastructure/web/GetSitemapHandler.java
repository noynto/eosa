package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadSitemap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record GetSitemapHandler(ReadSitemap readSitemap, String baseUrl, GiftCardOffer giftCardOffer) implements Handler {

    private static final List<String> STATIC_PATHS = List.of(
            "/", "/jewels", "/jewels/necklaces", "/jewels/bracelets", "/about", "/workshops",
            "/faq", "/contact", "/shipping", "/returns", "/legal", "/terms", "/privacy"
    );

    @Override
    public void handle(Context ctx) {
        var sitemap = readSitemap.handle();

        Map<String, Object> model = new HashMap<>();
        // La page d'achat d'une carte cadeau n'est dans le sitemap que quand l'offre est ouverte.
        var paths = new ArrayList<>(STATIC_PATHS);
        if (giftCardOffer.enabled()) {
            paths.add("/gift-cards");
        }
        model.put("staticUrls", paths.stream().map(path -> baseUrl + path).toList());
        model.put("jewelUrls", sitemap.jewelIds().stream().map(id -> baseUrl + "/jewels/" + id.value()).toList());
        model.put("workshopSessionUrls", sitemap.workshopSessionIds().stream()
                .map(id -> baseUrl + "/workshops/sessions/" + id.value())
                .toList());
        ctx.render("sitemap.xml.mustache", model);
        ctx.contentType("application/xml");
    }

}
