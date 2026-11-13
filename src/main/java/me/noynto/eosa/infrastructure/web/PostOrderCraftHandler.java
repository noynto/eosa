package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.CraftOrder;
import me.noynto.eosa.shared.OrderId;

/**
 * « Pièce fabriquée, à expédier » : crée l'étiquette chez le transporteur avec le poids et les dimensions du colis
 * saisis, et passe la commande à fabriquée. En cas d'échec, le message s'affiche dans le fragment d'erreur de la page
 * et la commande reste à fabriquer.
 */
public record PostOrderCraftHandler(
        CraftOrder craftOrder
) implements Handler {

    @Override
    public void handle(Context ctx) {
        Integer weightGrams = integer(ctx, "weight");
        Integer lengthCm = integer(ctx, "length");
        Integer widthCm = integer(ctx, "width");
        Integer depthCm = integer(ctx, "depth");
        if (weightGrams == null) {
            InlineError.render(ctx, "Le poids du colis doit être un nombre entier de grammes.");
            return;
        }
        if (lengthCm == null || widthCm == null || depthCm == null) {
            InlineError.render(ctx, "La longueur, la largeur et la profondeur du colis doivent être des nombres entiers de centimètres.");
            return;
        }
        try {
            craftOrder.handle(new CraftOrder.Command(new OrderId(ctx.pathParam("id")), weightGrams, lengthCm, widthCm, depthCm));
            ctx.header("HX-Refresh", "true");
            ctx.status(200);
        } catch (RuntimeException e) {
            InlineError.render(ctx, e);
        }
    }

    /** Le champ de formulaire en entier, null s'il manque ou n'en est pas un. */
    private static Integer integer(Context ctx, String name) {
        String value = ctx.formParam(name);
        try {
            return value == null ? null : Integer.valueOf(value.strip());
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
