package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Cookie;
import io.javalin.http.Handler;
import io.javalin.http.SameSite;
import me.noynto.eosa.application.AuthenticateIdentity;
import me.noynto.eosa.identity.IdentitySession;

import java.time.Duration;

/**
 * @param secureCookie send the session cookie over HTTPS only; true when the administration
 *                     is served over HTTPS (EOSA_ADMIN_BASE_URL).
 */
public record PostSignInHandler(
        AuthenticateIdentity authenticateIdentity,
        boolean secureCookie
) implements Handler {

    // Same lifetime as the identity session (see EnsureIdentityHasValidSession).
    private static final int SESSION_MAX_AGE_SECONDS = (int) Duration.ofHours(3).toSeconds();

    @Override
    public void handle(Context ctx) {
        try {
            IdentitySession session = authenticateIdentity.handle(
                    new AuthenticateIdentity.Command(ctx.formParam("name"), ctx.formParam("secret"))
            );
            ctx.cookie(new Cookie(
                    "identity-session-id",
                    session.getId().value(),
                    "/",
                    SESSION_MAX_AGE_SECONDS,
                    secureCookie,
                    true,
                    null,
                    SameSite.STRICT
            ));
            ctx.redirect("/jewels");
        } catch (AuthenticateIdentity.InvalidCredentials e) {
            ctx.redirect("/sign-in?error=1");
        }
    }

}
