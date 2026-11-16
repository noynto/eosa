package me.noynto.eosa.service;

import me.noynto.eosa.promo.PromoCode;
import me.noynto.eosa.promo.PromoCodeType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.Locale;

/**
 * Remise d'un code de promotion sur un panier. La règle croise le code, le panier et les commandes déjà passées
 * (le nombre d'usages) : c'est un service statique, sans provider ; le cas d'usage lui donne ce qu'il a lu.
 * La remise porte sur les bijoux, jamais sur la livraison.
 */
public final class PromoDiscounts {

    /** Stripe refuse un paiement de moins de 0,50 € : la remise est plafonnée pour laisser ce minimum. */
    public static final BigDecimal MINIMUM_PAYABLE = new BigDecimal("0.50");

    private PromoDiscounts() {
    }

    /**
     * @param code           le code saisi, null s'il n'existe pas
     * @param itemsAmount    total des bijoux, avant remise
     * @param shippingAmount frais de livraison du panier
     * @param uses           commandes déjà passées avec ce code, annulées comprises
     */
    public static Evaluation evaluate(PromoCode code, BigDecimal itemsAmount, BigDecimal shippingAmount, int uses, Instant now) {
        // Un code désactivé ne se distingue pas d'un code inconnu : on ne révèle pas les codes qui existent.
        if (code == null || !code.isActive()) {
            return Evaluation.refused("Ce code n'est pas valable.");
        }
        if (code.getEndsAt() != null && !now.isBefore(code.getEndsAt())) {
            return Evaluation.refused("Ce code a expiré.");
        }
        if (code.getStartsAt() != null && now.isBefore(code.getStartsAt())) {
            return Evaluation.refused("Ce code n'est pas encore valable.");
        }
        if (code.getMaxUses() != null && uses >= code.getMaxUses()) {
            return Evaluation.refused("Ce code n'est plus disponible.");
        }
        if (code.getMinimumAmount() != null && itemsAmount.compareTo(code.getMinimumAmount()) < 0) {
            return Evaluation.refused("Ce code s'applique dès " + NumberFormat.getCurrencyInstance(Locale.FRANCE)
                    .format(code.getMinimumAmount()) + " de bijoux.");
        }

        BigDecimal discount = code.getType() == PromoCodeType.PERCENT
                ? itemsAmount.multiply(code.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : code.getValue();
        BigDecimal payableCap = itemsAmount.add(shippingAmount).subtract(MINIMUM_PAYABLE);
        discount = discount.min(itemsAmount).min(payableCap).setScale(2, RoundingMode.HALF_UP);
        if (discount.signum() <= 0) {
            return Evaluation.refused("Ce code ne s'applique pas à ce panier.");
        }
        return new Evaluation(discount, null);
    }

    /**
     * @param discount remise accordée, zéro quand le code est refusé
     * @param refusal  la raison du refus, à montrer à la cliente ; null quand le code s'applique
     */
    public record Evaluation(BigDecimal discount, String refusal) {

        static Evaluation refused(String refusal) {
            return new Evaluation(BigDecimal.ZERO, refusal);
        }

        public boolean applies() {
            return refusal == null;
        }
    }

}
