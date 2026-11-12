package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.CraftOrder;
import me.noynto.eosa.shared.OrderId;

/**
 * « Pièce fabriquée » : crée l'étiquette chez le transporteur et passe la commande à fabriquée. En cas d'échec, le
 * message s'affiche dans le fragment d'erreur de la page et la commande reste à fabriquer.
 */
public record PostOrderCraftHandler(
        CraftOrder craftOrder
) implements Handler {

    @Override
    public void handle(Context ctx) {
        int weightGrams;
        try {
            weightGrams = Integer.parseInt(ctx.formParam("weight") == null ? "" : ctx.formParam("weight").strip());
        } catch (NumberFormatException e) {
            InlineError.render(ctx, "Le poids du colis doit être un nombre entier de grammes.");
            return;
        }
        try {
            craftOrder.handle(new CraftOrder.Command(new OrderId(ctx.pathParam("id")), weightGrams));
            ctx.header("HX-Refresh", "true");
            ctx.status(200);
        } catch (RuntimeException e) {
            InlineError.render(ctx, e);
        }
    }

}
