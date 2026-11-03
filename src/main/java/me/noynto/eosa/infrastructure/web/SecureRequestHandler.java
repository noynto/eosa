package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;

import java.net.URI;
import java.util.Set;

/**
 * Runs before every route of a server. Sets the security headers and refuses a request that changes data
 * (POST, PATCH, PUT, DELETE) when the browser says it comes from another site: protection against
 * cross-site request forgery (CSRF).
 * <p>
 * The session cookie of the administration is already {@code SameSite=Strict}, but eosa.me and admin.eosa.me are
 * the same "site" for the browser: SameSite does not protect one from the other. The {@code Origin} header does.
 */
public record SecureRequestHandler(String allowedOrigin) implements Handler {

    private static final Set<HandlerType> SAFE_METHODS = Set.of(HandlerType.GET, HandlerType.HEAD, HandlerType.OPTIONS);

    public static SecureRequestHandler of(URI baseUrl) {
        return new SecureRequestHandler(baseUrl.getScheme() + "://" + baseUrl.getAuthority());
    }

    @Override
    public void handle(Context ctx) {
        ctx.header("X-Content-Type-Options", "nosniff");
        ctx.header("X-Frame-Options", "DENY");
        ctx.header("Referrer-Policy", "strict-origin-when-cross-origin");
        if (SAFE_METHODS.contains(ctx.method())) {
            return;
        }
        // Every current browser sends Origin on these requests; Sec-Fetch-Site covers the others.
        // A request without either does not come from a browser, so it carries no cookie by itself.
        String origin = ctx.header("Origin");
        if (origin != null) {
            if (!origin.equalsIgnoreCase(allowedOrigin)) {
                throw new ForbiddenResponse("Requête refusée : elle ne vient pas de ce site.");
            }
            return;
        }
        String fetchSite = ctx.header("Sec-Fetch-Site");
        if (fetchSite != null && !fetchSite.equals("same-origin") && !fetchSite.equals("none")) {
            throw new ForbiddenResponse("Requête refusée : elle ne vient pas de ce site.");
        }
    }

}
