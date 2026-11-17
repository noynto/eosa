package me.noynto.eosa.infrastructure.web;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Mise en forme des prix de la boutique pour les templates (Mustache n'a pas de logique). */
final class PriceFormat {

    private PriceFormat() {
    }

    /** « 42 » pour un montant rond, « 4,90 » sinon : la virgule du français, deux décimales. */
    static String price(BigDecimal amount) {
        BigDecimal stripped = amount.stripTrailingZeros();
        if (stripped.scale() <= 0) {
            return stripped.toPlainString();
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

}
