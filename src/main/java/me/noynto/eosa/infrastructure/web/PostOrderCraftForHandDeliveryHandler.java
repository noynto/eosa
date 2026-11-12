package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.CraftOrderForHandDelivery;
import me.noynto.eosa.shared.OrderId;

/**
 * « Remise en main propre » à la fabrication : la commande passe à fabriquée sans étiquette ni transporteur.
 */
public record PostOrderCraftForHandDeliveryHandler(
        CraftOrderForHandDelivery craftOrderForHandDelivery
) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            craftOrderForHandDelivery.handle(new CraftOrderForHandDelivery.Command(new OrderId(ctx.pathParam("id"))));
            ctx.header("HX-Refresh", "true");
            ctx.status(200);
        } catch (RuntimeException e) {
            InlineError.render(ctx, e);
        }
    }

}
