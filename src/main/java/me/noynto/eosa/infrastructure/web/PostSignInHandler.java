package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.AuthenticateIdentity;
import me.noynto.eosa.identity.IdentitySession;

public record PostSignInHandler(
        AuthenticateIdentity authenticateIdentity
) implements Handler {

    @Override
    public void handle(Context ctx) {
        try {
            IdentitySession session = authenticateIdentity.handle(
                    new AuthenticateIdentity.Command(ctx.formParam("name"), ctx.formParam("secret"))
            );
            ctx.cookie("identity-session-id", session.getId().value());
            ctx.redirect("/admin/products");
        } catch (AuthenticateIdentity.InvalidCredentials e) {
            ctx.redirect("/sign-in?error=1");
        }
    }

}