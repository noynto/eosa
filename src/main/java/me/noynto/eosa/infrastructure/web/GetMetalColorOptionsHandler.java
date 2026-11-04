package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadMetalColors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment du formulaire d'ajout au panier : le choix de la couleur du métal (la première est cochée), vide
 * s'il n'y en a pas. Les couleurs sont les mêmes pour tous les bijoux.
 */
public record GetMetalColorOptionsHandler(ReadMetalColors readMetalColors) implements Handler {

    @Override
    public void handle(Context ctx) {
        var metalColors = readMetalColors.handle();
        List<Map<String, Object>> options = new ArrayList<>();
        for (int i = 0; i < metalColors.size(); i++) {
            var metalColor = metalColors.get(i);
            boolean hasImage = metalColor.getImageId() != null;
            Map<String, Object> option = new HashMap<>();
            option.put("id", metalColor.getId().value());
            option.put("name", metalColor.getName());
            option.put("hasImage", hasImage);
            option.put("imageId", hasImage ? metalColor.getImageId().value() : "");
            option.put("selected", i == 0);
            options.add(option);
        }
        Map<String, Object> model = new HashMap<>();
        model.put("hasMetalColors", !options.isEmpty());
        model.put("metalColors", options);
        ctx.render("partials/metal-color-options.mustache", model);
    }

}
