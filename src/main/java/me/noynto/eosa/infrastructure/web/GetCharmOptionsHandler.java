package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadCharms;

import java.util.HashMap;
import java.util.Map;

/**
 * Fragment du formulaire d'ajout au panier : le choix des breloques, vide s'il n'y en a pas. Les breloques sont
 * les mêmes pour tous les bijoux.
 */
public record GetCharmOptionsHandler(ReadCharms readCharms) implements Handler {

    @Override
    public void handle(Context ctx) {
        var options = readCharms.handle().charms().stream().map(charm -> {
            boolean hasImage = charm.getImageId() != null;
            Map<String, Object> option = new HashMap<>();
            option.put("id", charm.getId().value());
            option.put("name", charm.getName());
            option.put("hasImage", hasImage);
            option.put("imageId", hasImage ? charm.getImageId().value() : "");
            option.put("price", charm.getPrice().stripTrailingZeros().toPlainString());
            return option;
        }).toList();
        Map<String, Object> model = new HashMap<>();
        model.put("hasCharms", !options.isEmpty());
        model.put("charms", options);
        ctx.render("partials/charm-options.mustache", model);
    }

}
