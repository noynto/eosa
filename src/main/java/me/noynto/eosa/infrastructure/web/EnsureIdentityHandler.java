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
        var command = new EnsureIdentityHasValidSession.Command(new IdentitySessionId(ctx.cookie("identity-session-id")));
        ensureIdentityHasValidSession.handle(command).ifPresentOrElse(
                identity -> ctx.attribute("identityId", identity.getId()),
                () -> ctx.redirect("/sign-in")
        );
    }

}