package me.noynto.eosa.infrastructure.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * L'offre publique de cartes cadeau : ses montants proposés, ses bornes et sa validité, lus dans la configuration.
 */
public record GiftCardOffer(
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

    /** Ce que les pages de texte (CGV, FAQ) lisent de l'offre. */
    public Map<String, Object> textModel() {
        return Map.of(
                "giftCardMinimum", minimumAmount.intValue(),
                "giftCardMaximum", maximumAmount.intValue(),
                "giftCardValidity", validityLabel()
        );
    }

}
