package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;

import java.util.HashMap;
import java.util.Map;

/**
 * Réponse des formulaires publics d'une session (réservation, liste d'attente). Envoyés par htmx, ils reçoivent
 * un fragment : l'erreur s'affiche sous le formulaire, qui garde ce qui a été saisi, et la confirmation
 * remplace le formulaire. Sans htmx (JavaScript absent), une page simple porte le même message.
 * <p>
 * Les messages passent par Mustache, qui les échappe : ils peuvent citer une valeur saisie.
 */
final class WorkshopFormResponse {

    private WorkshopFormResponse() {
    }

    static boolean isHtmx(Context ctx) {
        return ctx.header("HX-Request") != null;
    }

    static void error(Context ctx, String baseUrl, String message) {
        if (isHtmx(ctx)) {
            ctx.header("HX-Retarget", "#form-error");
            ctx.header("HX-Reswap", "innerHTML");
            ctx.status(422).render("partials/workshop-form-error.mustache", Map.of("message", message));
            return;
        }
        page(ctx, baseUrl, "Demande refusée", message, true);
    }

    /**
     * La page simple, pour une requête qui ne vient pas de htmx.
     */
    static void page(Context ctx, String baseUrl, String heading, String message, boolean failed) {
        String sessionId = ctx.pathParam("id");
        Map<String, Object> model = new HashMap<>();
        model.put("title", "Eosa — " + heading);
        model.put("description", heading);
        model.put("ogImageUrl", baseUrl + "/workshops.webp");
        model.put("canonicalUrl", baseUrl + "/workshops/sessions/" + sessionId);
        model.put("noindex", true);
        model.put("sessionId", sessionId);
        model.put("heading", heading);
        model.put("message", message);
        if (failed) {
            ctx.status(422);
        }
        ctx.render("workshop-form-result.mustache", model);
    }

}
