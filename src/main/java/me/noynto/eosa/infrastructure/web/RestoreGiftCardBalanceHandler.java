package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.RestoreGiftCardBalance;
import me.noynto.eosa.shared.GiftCardId;

public record RestoreGiftCardBalanceHandler(RestoreGiftCardBalance restoreGiftCardBalance) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            restoreGiftCardBalance.handle(new RestoreGiftCardBalance.Command(
                    new GiftCardId(ctx.pathParam("id")),
                    PromoCodeForm.decimal(ctx.formParam("amount"), "Le montant"),
                    ctx.formParam("reason")
            ));
            ctx.header("HX-Redirect", "/gift-cards/" + ctx.pathParam("id"));
            ctx.status(200);
        } catch (RuntimeException e) {
            InlineError.render(ctx, e);
        }
    }

}
