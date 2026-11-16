package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.DeletePromoCode;
import me.noynto.eosa.shared.PromoCodeId;

public record DeletePromoCodeHandler(DeletePromoCode deletePromoCode) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            deletePromoCode.handle(new DeletePromoCode.Command(new PromoCodeId(ctx.pathParam("id"))));
            ctx.header("HX-Redirect", "/promo-codes");
            ctx.status(200);
        } catch (RuntimeException e) {
            InlineError.render(ctx, e);
        }
    }

}
