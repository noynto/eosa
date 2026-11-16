package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.VoidGiftCard;
import me.noynto.eosa.shared.GiftCardId;

public record VoidGiftCardHandler(VoidGiftCard voidGiftCard) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            voidGiftCard.handle(new VoidGiftCard.Command(new GiftCardId(ctx.pathParam("id")), ctx.formParam("reason")));
            ctx.header("HX-Redirect", "/gift-cards/" + ctx.pathParam("id"));
            ctx.status(200);
        } catch (RuntimeException e) {
            InlineError.render(ctx, e);
        }
    }

}
