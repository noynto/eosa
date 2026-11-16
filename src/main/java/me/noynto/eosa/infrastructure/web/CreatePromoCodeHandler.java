package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.CreatePromoCode;

public record CreatePromoCodeHandler(CreatePromoCode createPromoCode) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            var form = PromoCodeForm.read(ctx);
            var promoCode = createPromoCode.handle(new CreatePromoCode.Command(
                    ctx.formParam("code"),
                    form.type(),
                    form.value(),
                    form.startsOn(),
                    form.endsOn(),
                    form.maxUses(),
                    form.minimumAmount()
            )).promoCode();
            ctx.header("HX-Redirect", "/promo-codes/" + promoCode.getId().value());
            ctx.status(200);
        } catch (RuntimeException e) {
            InlineError.render(ctx, e);
        }
    }

}
