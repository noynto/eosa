package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;

public record GetGiftCardPurchaseHandler(GiftCardPurchasePage page) implements Handler {

    @Override
    public void handle(Context ctx) {
        if (!page.offer().enabled()) {
            ctx.status(404);
            return;
        }
        page.render(ctx, null, null, null, null, null);
    }

}
