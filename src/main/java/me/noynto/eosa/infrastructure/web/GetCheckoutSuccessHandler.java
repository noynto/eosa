package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ConfirmCheckoutSession;
import me.noynto.eosa.shared.CheckoutSessionId;

import java.util.Map;

public record GetCheckoutSuccessHandler(
        ConfirmCheckoutSession confirmCheckoutSession
) implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        String stripeSessionId = ctx.queryParam("session_id");
        CheckoutSessionId checkoutSessionId = new CheckoutSessionId();
        checkoutSessionId.setStripe(stripeSessionId);
        String cartCookie = ctx.cookie("cart");
        var session = confirmCheckoutSession.handle(new ConfirmCheckoutSession.Command(checkoutSessionId));
        ctx.cookie("cart", "");
        ctx.render("checkout/success.jte", Map.of("checkout", session));
    }

}