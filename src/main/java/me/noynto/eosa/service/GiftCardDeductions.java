package me.noynto.eosa.service;

import me.noynto.eosa.giftcard.GiftCard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Part d'un panier qu'une carte cadeau paie. La règle croise la carte et le panier : c'est un service statique,
 * sans provider ; le cas d'usage lui donne ce qu'il a lu. La carte paie les bijoux seulement, jamais la livraison.
 */
public final class GiftCardDeductions {

    /** Stripe refuse un total entre 0 et 0,50 € (il accepte 0 €) : la carte déduit moins pour laisser ce minimum. */
    public static final BigDecimal MINIMUM_PAYABLE = PromoDiscounts.MINIMUM_PAYABLE;

    private GiftCardDeductions() {
    }

    /**
     * @param giftCard       la carte saisie, null si elle n'existe pas
     * @param itemsAmount    total des bijoux
     * @param shippingAmount frais de livraison du panier
     */
    public static Evaluation evaluate(GiftCard giftCard, BigDecimal itemsAmount, BigDecimal shippingAmount, Instant now) {
        // Inconnue, annulée, expirée ou soldée : le même message, sans révéler laquelle.
        if (giftCard == null || !giftCard.isUsable(now)) {
            return Evaluation.refused("Ce code n'est pas valable.");
        }
        BigDecimal deduction = giftCard.balance().min(itemsAmount);
        BigDecimal remaining = itemsAmount.add(shippingAmount).subtract(deduction);
        if (remaining.signum() > 0 && remaining.compareTo(MINIMUM_PAYABLE) < 0) {
            deduction = itemsAmount.add(shippingAmount).subtract(MINIMUM_PAYABLE);
        }
        deduction = deduction.setScale(2, RoundingMode.HALF_UP);
        if (deduction.signum() <= 0) {
            return Evaluation.refused("Cette carte ne peut pas s'appliquer à ce panier.");
        }
        return new Evaluation(deduction, null);
    }

    /**
     * @param deduction part payée par la carte, zéro quand elle est refusée
     * @param refusal   la raison du refus, à montrer à la cliente ; null quand la carte s'applique
     */
    public record Evaluation(BigDecimal deduction, String refusal) {

        static Evaluation refused(String refusal) {
            return new Evaluation(BigDecimal.ZERO, refusal);
        }

        public boolean applies() {
            return refusal == null;
        }
    }

}
