package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.EnsureIdentityHasValidSession;
import me.noynto.eosa.shared.IdentitySessionId;

public record EnsureIdentityHandler(
        EnsureIdentityHasValidSession ensureIdentityHasValidSession
) implements Handler {

    @Override
    public void handle(Context ctx) {
        EnsureIdentityHasValidSession.Command command = new EnsureIdentityHasValidSession.Command(new IdentitySessionId(ctx.cookie("identity-session-id")));
        if (ensureIdentityHasValidSession.handle(command)) {
            ctx.redirect("/sign-in");
        }
        IdentitySessionId identitySessionId = new IdentitySessionId(null);


      /*  var cart = getOrCreateCart.handle(new GetOrCreateCart.Command(
                cookieValue != null ? new CartId(cookieValue) : null
        ));
        ctx.cookie("cart", cart.getId().value());*/
    }

}