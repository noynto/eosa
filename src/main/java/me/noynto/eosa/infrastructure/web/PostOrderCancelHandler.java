package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.CancelOrder;
import me.noynto.eosa.shared.OrderId;

/**
 * Annule une commande avec le motif saisi. Aucun e-mail n'est envoyé et rien n'est remboursé.
 */
public record PostOrderCancelHandler(
        CancelOrder cancelOrder
) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            cancelOrder.handle(new CancelOrder.Command(new OrderId(ctx.pathParam("id")), ctx.formParam("reason")));
            ctx.header("HX-Refresh", "true");
            ctx.status(200);
        } catch (RuntimeException e) {
            InlineError.render(ctx, e);
        }
    }

}
