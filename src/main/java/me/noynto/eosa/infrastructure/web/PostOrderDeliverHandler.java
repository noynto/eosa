package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.DeliverOrder;
import me.noynto.eosa.shared.OrderId;

/**
 * « Livrée » : la commande expédiée passe à livrée.
 */
public record PostOrderDeliverHandler(
        DeliverOrder deliverOrder
) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            deliverOrder.handle(new DeliverOrder.Command(new OrderId(ctx.pathParam("id"))));
            ctx.header("HX-Refresh", "true");
            ctx.status(200);
        } catch (RuntimeException e) {
            InlineError.render(ctx, e);
        }
    }

}
