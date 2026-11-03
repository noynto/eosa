package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;

import java.util.Map;
import java.util.Objects;

/**
 * Renders an error message as an htmx fragment (status 422). The message goes through Mustache, which escapes it:
 * it may quote a value from the request (an identifier from the URL, a form field) and must never be written
 * into the HTML as is.
 */
final class InlineError {

    private InlineError() {
    }

    static void render(Context ctx, RuntimeException e) {
        render(ctx, e.getMessage());
    }

    static void render(Context ctx, String message) {
        ctx.status(422).render(
                "partials/inline-error.mustache",
                Map.of("message", Objects.requireNonNullElse(message, "Une erreur est survenue."))
        );
    }

}
