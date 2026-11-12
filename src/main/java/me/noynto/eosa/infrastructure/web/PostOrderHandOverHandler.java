package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.HandOverOrder;
import me.noynto.eosa.shared.OrderId;

/**
 * « Remise effectuée » : la commande fabriquée pour une remise en main propre passe à livrée.
 */
public record PostOrderHandOverHandler(
        HandOverOrder handOverOrder
) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            handOverOrder.handle(new HandOverOrder.Command(new OrderId(ctx.pathParam("id"))));
            ctx.header("HX-Refresh", "true");
            ctx.status(200);
        } catch (RuntimeException e) {
            InlineError.render(ctx, e);
        }
    }

}
