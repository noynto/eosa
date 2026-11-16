package me.noynto.eosa.infrastructure.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * L'offre publique de cartes cadeau : ses montants, sa validité, et surtout son interrupteur. Tant que
 * {@code enabled} est faux (cas par défaut), la vente n'existe pas pour le public : la page d'achat répond 404, elle
 * n'est pas dans le sitemap, et les textes (CGV, FAQ, confidentialité) n'en parlent pas. L'utilisation d'une carte au
 * panier, elle, ne dépend pas de l'interrupteur (une carte offerte par l'administration doit rester utilisable).
 */
public record GiftCardOffer(
        boolean enabled,
        List<Integer> presetAmounts,
        BigDecimal minimumAmount,
        BigDecimal maximumAmount,
        int validityDays
) {

    /** « un an », « 2 ans » ou « 90 jours ». */
    public String validityLabel() {
        return validityDays % 365 == 0
                ? (validityDays / 365 == 1 ? "un an" : (validityDays / 365) + " ans")
                : validityDays + " jours";
    }

    /** Ce que les pages de texte (CGV, FAQ, confidentialité) lisent de l'offre. */
    public Map<String, Object> textModel() {
        return Map.of(
                "giftCardsEnabled", enabled,
                "giftCardMinimum", minimumAmount.intValue(),
                "giftCardMaximum", maximumAmount.intValue(),
                "giftCardValidity", validityLabel()
        );
    }

}
