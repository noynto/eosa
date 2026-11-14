package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.SetPromoCodeActive;
import me.noynto.eosa.shared.PromoCodeId;

/** Active ou désactive un code ; {@code active} dit l'état voulu, la route (activate ou deactivate) le fixe. */
public record SetPromoCodeActiveHandler(SetPromoCodeActive setPromoCodeActive, boolean active) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            setPromoCodeActive.handle(new SetPromoCodeActive.Command(new PromoCodeId(ctx.pathParam("id")), active));
            ctx.header("HX-Redirect", "/promo-codes/" + ctx.pathParam("id"));
            ctx.status(200);
        } catch (RuntimeException e) {
            InlineError.render(ctx, e);
        }
    }

}
