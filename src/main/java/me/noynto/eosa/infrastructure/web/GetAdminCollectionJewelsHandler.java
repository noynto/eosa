package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadCollectionJewelChoices;
import me.noynto.eosa.jewel.JewelState;
import me.noynto.eosa.shared.CollectionId;

import java.util.HashMap;
import java.util.Map;

/** Le bloc « Bijoux » de la fiche d'une collection, chargé par htmx. */
public record GetAdminCollectionJewelsHandler(ReadCollectionJewelChoices readCollectionJewelChoices) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            var collectionId = new CollectionId(ctx.pathParam("id"));
            var choices = readCollectionJewelChoices.handle(new ReadCollectionJewelChoices.Command(collectionId)).choices();

            var rows = choices.stream().map(choice -> {
                var jewel = choice.jewel();
                boolean hasImage = !jewel.getImageIds().isEmpty();
                Map<String, Object> row = new HashMap<>();
                row.put("id", jewel.getId().value());
                row.put("name", jewel.getName() != null && !jewel.getName().isBlank() ? jewel.getName() : "Sans nom");
                row.put("stateLabel", stateLabel(jewel.getState()));
                row.put("selected", choice.selected());
                row.put("hasImage", hasImage);
                row.put("imageId", hasImage ? jewel.getImageIds().get(0).value() : "");
                return row;
            }).toList();

            Map<String, Object> model = new HashMap<>();
            model.put("collectionId", collectionId.value());
            model.put("jewels", rows);
            model.put("hasJewels", !rows.isEmpty());
            ctx.render("admin/partials/collection-jewels.mustache", model);
        } catch (RuntimeException e) {
            ctx.status(404);
        }
    }

    private static String stateLabel(JewelState state) {
        if (state == JewelState.PUBLISHED) {
            return "Publié";
        }
        if (state == JewelState.ARCHIVED) {
            return "Archivé";
        }
        return "Brouillon";
    }

}
