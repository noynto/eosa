package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadJewelIds;
import me.noynto.eosa.jewel.JewelState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record GetSitemapHandler(ReadJewelIds readJewelIds, String baseUrl) implements Handler {

    private static final List<String> STATIC_PATHS = List.of(
            "/", "/jewels", "/jewels/necklaces", "/jewels/bracelets", "/legal", "/terms", "/privacy"
    );

    @Override
    public void handle(Context ctx) {
        var jewelIds = readJewelIds.handle(new ReadJewelIds.Query(Set.of(JewelState.PUBLISHED), Set.of()));

        Map<String, Object> model = new HashMap<>();
        model.put("staticUrls", STATIC_PATHS.stream().map(path -> baseUrl + path).toList());
        model.put("jewelUrls", jewelIds.stream().map(id -> baseUrl + "/jewels/" + id.value()).toList());
        ctx.render("sitemap.xml.mustache", model);
        ctx.contentType("application/xml");
    }

}
