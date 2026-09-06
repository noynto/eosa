package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadJewel;
import me.noynto.eosa.application.ReadJewelIds;
import me.noynto.eosa.application.ReadMetalColors;
import me.noynto.eosa.jewel.JewelState;
import me.noynto.eosa.shared.JewelId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record GetJewelHandler(ReadJewel readJewel, ReadJewelIds readJewelIds, ReadMetalColors readMetalColors, String baseUrl) implements Handler {

    private static final String THUMB_ACTIVE = "w-16 h-16 bg-bg-soft rounded-xl border-2 border-primary relative overflow-hidden flex-shrink-0 cursor-pointer";
    private static final String THUMB_INACTIVE = "w-16 h-16 bg-bg-soft rounded-xl relative overflow-hidden flex-shrink-0 cursor-pointer";

    @Override
    public void handle(Context ctx) throws Exception {
        try {
            var jewelId = new JewelId(ctx.pathParam("id"));
            var jewel = readJewel.handle(new ReadJewel.Command(jewelId));
            var allIds = new ArrayList<>(readJewelIds.handle(new ReadJewelIds.Query(Set.of(JewelState.PUBLISHED), Set.of())));
            allIds.remove(jewelId);
            Collections.shuffle(allIds);
            var relatedIds = allIds.stream().limit(4).toList();

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
            model.put("jewelId", jewel.getId().value());
            model.put("hasImages", hasImages);
            model.put("mainImageId", hasImages ? jewel.getImageIds().getFirst().value() : "");
            model.put("images", images);
            model.put("hasRelated", !relatedIds.isEmpty());
            model.put("relatedIds", relatedIds.stream().map(id -> Map.of("id", id.value())).toList());

            var metalColors = readMetalColors.handle();
            List<Map<String, Object>> metalColorOptions = new ArrayList<>();
            for (int i = 0; i < metalColors.size(); i++) {
                var metalColor = metalColors.get(i);
                boolean hasMetalColorImage = metalColor.getImageId() != null;
                Map<String, Object> option = new HashMap<>();
                option.put("id", metalColor.getId().value());
                option.put("name", metalColor.getName());
                option.put("hasImage", hasMetalColorImage);
                option.put("imageId", hasMetalColorImage ? metalColor.getImageId().value() : "");
                option.put("selected", i == 0);
                metalColorOptions.add(option);
            }
            model.put("hasMetalColors", !metalColorOptions.isEmpty());
            model.put("metalColors", metalColorOptions);

            ctx.render("jewel.mustache", model);
        } catch (RuntimeException e) {
            ctx.status(404);
        }
    }

}