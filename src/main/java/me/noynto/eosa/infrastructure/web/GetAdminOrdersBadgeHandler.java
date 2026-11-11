package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadOrders;
import me.noynto.eosa.order.OrderState;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Pastille du menu : le nombre de commandes à fabriquer, chargée par htmx à chaque page. Rien à afficher (204)
 * quand il n'y en a aucune.
 */
public record GetAdminOrdersBadgeHandler(
        ReadOrders readOrders
) implements Handler {

    @Override
    public void handle(Context ctx) {
        int toCraft = readOrders.handle(new ReadOrders.Command(EnumSet.of(OrderState.PAID), null)).orders().size();
        if (toCraft == 0) {
            ctx.status(204);
            return;
        }
        Map<String, Object> model = new HashMap<>();
        model.put("count", toCraft);
        ctx.render("admin/partials/orders-badge.mustache", model);
    }

}
