package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ShipOrder;
import me.noynto.eosa.shared.OrderId;

/**
 * « Colis remis » : la commande fabriquée passe à expédiée et la cliente reçoit son e-mail de suivi.
 */
public record PostOrderShipHandler(
        ShipOrder shipOrder
) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            shipOrder.handle(new ShipOrder.Command(new OrderId(ctx.pathParam("id"))));
            ctx.header("HX-Refresh", "true");
            ctx.status(200);
        } catch (RuntimeException e) {
            InlineError.render(ctx, e);
        }
    }

}
