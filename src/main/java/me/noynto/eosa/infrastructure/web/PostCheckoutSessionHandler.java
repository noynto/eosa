package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import me.noynto.eosa.application.InitiateCheckout;
import me.noynto.eosa.shared.CartId;

public record PostCheckoutSessionHandler(
        InitiateCheckout initiateCheckout
) implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        String cartCookie = ctx.cookie("cart");
        if (cartCookie == null) {
            ctx.redirect("/cart", HttpStatus.SEE_OTHER);
            return;
        }
        var session = initiateCheckout.handle(
                new InitiateCheckout.Command(new CartId(cartCookie))
        );
        ctx.redirect(session.getSession().getUri().toString(), HttpStatus.SEE_OTHER);
    }

}