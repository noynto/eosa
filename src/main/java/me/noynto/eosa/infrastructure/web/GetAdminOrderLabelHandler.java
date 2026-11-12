package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import me.noynto.eosa.application.ReadOrderLabel;
import me.noynto.eosa.shared.OrderId;

import java.net.URI;

/**
 * PDF de l'étiquette d'une commande : redirige vers l'adresse protégée que le transporteur a rendue à la création.
 * Ne modifie rien.
 */
public record GetAdminOrderLabelHandler(
        ReadOrderLabel readOrderLabel
) implements Handler {

    @Override
    public void handle(Context ctx) {
        String url;
        try {
            url = readOrderLabel.handle(new ReadOrderLabel.Command(new OrderId(ctx.pathParam("id")))).url();
        } catch (RuntimeException e) {
            ctx.status(404);
            return;
        }
        // Seule une adresse web est suivie, quoi que contienne la base.
        String scheme;
        try {
            scheme = url == null ? null : URI.create(url).getScheme();
        } catch (IllegalArgumentException e) {
            scheme = null;
        }
        if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
            ctx.status(404);
            return;
        }
        ctx.redirect(url);
    }

}
