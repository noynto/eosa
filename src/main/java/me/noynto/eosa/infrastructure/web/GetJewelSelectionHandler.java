package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadJewelSelection;
import me.noynto.eosa.shared.CharmId;
import me.noynto.eosa.shared.JewelId;
import me.noynto.eosa.shared.MetalColorId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment de la fiche d'un bijou : « Votre sélection » (métal, breloques) et le prix total. Le formulaire
 * le recharge à chaque changement d'un choix.
 */
public record GetJewelSelectionHandler(ReadJewelSelection readJewelSelection) implements Handler {

    @Override
    public void handle(Context ctx) {
        String metalColorId = ctx.queryParam("metalColorId");
        var charmIds = ctx.queryParams("charmIds").stream()
                .filter(value -> !value.isBlank())
                .map(CharmId::new)
                .toList();
        var result = readJewelSelection.handle(new ReadJewelSelection.Command(
                new JewelId(ctx.pathParam("id")),
                metalColorId != null && !metalColorId.isBlank() ? new MetalColorId(metalColorId) : null,
                charmIds
        ));

        List<String> parts = new ArrayList<>();
        if (result.metalColorName() != null) {
            parts.add(result.metalColorName());
        }
        parts.add(result.charmNames().isEmpty() ? "sans breloque" : String.join(" + ", result.charmNames()));

        Map<String, Object> model = new HashMap<>();
        model.put("summary", String.join(", ", parts));
        model.put("total", PriceFormat.price(result.total()));
        ctx.render("partials/jewel-selection.mustache", model);
    }

}
