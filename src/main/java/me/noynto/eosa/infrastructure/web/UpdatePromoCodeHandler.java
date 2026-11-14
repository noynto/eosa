package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.UpdatePromoCode;
import me.noynto.eosa.shared.PromoCodeId;

public record UpdatePromoCodeHandler(UpdatePromoCode updatePromoCode) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            var form = PromoCodeForm.read(ctx);
            updatePromoCode.handle(new UpdatePromoCode.Command(
                    new PromoCodeId(ctx.pathParam("id")),
                    form.type(),
                    form.value(),
                    form.startsOn(),
                    form.endsOn(),
                    form.maxUses(),
                    form.minimumAmount()
            ));
            ctx.html("<span class=\"text-success text-xs\">Sauvegardé</span>");
        } catch (RuntimeException e) {
            InlineError.render(ctx, e);
        }
    }

}
