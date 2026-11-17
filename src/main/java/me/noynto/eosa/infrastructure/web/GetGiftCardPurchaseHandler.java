package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;

public record GetGiftCardPurchaseHandler(GiftCardPurchasePage page) implements Handler {

    @Override
    public void handle(Context ctx) {
        page.render(ctx, null, null, null, null, null);
    }

}
