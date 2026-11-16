package me.noynto.eosa.infrastructure.web;

import io.javalin.http.Context;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * La page d'achat d'une carte cadeau, rendue par la page elle-même ({@link GetGiftCardPurchaseHandler}) et par
 * le formulaire qui la refuse ({@link PostGiftCardHandler}) : ce qui a été saisi reste dans les champs.
 */
public record GiftCardPurchasePage(
        String baseUrl,
        List<Integer> presetAmounts,
        BigDecimal minimumAmount,
        BigDecimal maximumAmount,
        int validityDays
) {

    void render(Context ctx, String error, String preset, String amount, String firstName, String email) {
        List<Map<String, Object>> presets = presetAmounts.stream().map(value -> {
            Map<String, Object> row = new HashMap<>();
            row.put("value", value);
            row.put("checked", String.valueOf(value).equals(preset));
            return row;
        }).toList();

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Eosa — Carte cadeau");
        model.put("description", "Offrez une carte cadeau EOSA : un montant à dépenser dans la boutique, reçu par e-mail.");
        model.put("ogImageUrl", baseUrl + "/hero.webp");
        model.put("canonicalUrl", baseUrl + "/gift-cards");
        // Ni indexée ni dans le sitemap, ni liée depuis le site, tant que les textes (CGV, FAQ) ne sont pas écrits
        // et validés : étape « Textes » de la spécification.
        model.put("noindex", true);
        model.put("presets", presets);
        model.put("minimum", minimumAmount.intValue());
        model.put("maximum", maximumAmount.intValue());
        model.put("validityLabel", validityDays % 365 == 0
                ? (validityDays / 365 == 1 ? "un an" : (validityDays / 365) + " ans")
                : validityDays + " jours");
        model.put("hasError", error != null);
        model.put("error", error);
        model.put("amount", amount == null ? "" : amount);
        model.put("firstName", firstName == null ? "" : firstName);
        model.put("email", email == null ? "" : email);
        if (error != null) {
            ctx.status(422);
        }
        ctx.render("gift-card-purchase.mustache", model);
    }

}
