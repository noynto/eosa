package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.EnsureIdentityHasValidSession;
import me.noynto.eosa.shared.IdentitySessionId;

/**
 * Guards every route of the administration server: only the sign-in page, images and
 * static assets are reachable without a valid identity session.
 */
public record EnsureIdentityHandler(
        EnsureIdentityHasValidSession ensureIdentityHasValidSession
) implements Handler {

    @Override
    public void handle(Context ctx) {
        if (isOpen(ctx.path())) {
            return;
        }
        var command = new EnsureIdentityHasValidSession.Command(new IdentitySessionId(ctx.cookie("identity-session-id")));
        ensureIdentityHasValidSession.handle(command).ifPresentOrElse(
                identity -> ctx.attribute("identityId", identity.getId()),
                () -> {
                    ctx.redirect("/sign-in");
                    // Without this the endpoint would still run after the redirect.
                    ctx.skipRemainingHandlers();
                }
        );
    }

    private boolean isOpen(String path) {
        return path.equals("/sign-in")
                || path.startsWith("/images/")
                || isStaticAsset(path);
    }

    private boolean isStaticAsset(String path) {
        return !path.endsWith("/") && getClass().getResource("/public" + path) != null;
    }

}
