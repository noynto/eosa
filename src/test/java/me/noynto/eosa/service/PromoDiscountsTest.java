package me.noynto.eosa.service;

import me.noynto.eosa.promo.PromoCode;
import me.noynto.eosa.promo.PromoCodeType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromoDiscountsTest {

    private final Instant now = Instant.parse("2026-11-14T10:00:00Z");
    private final BigDecimal shipping = new BigDecimal("4.90");

    private PromoCode percent(String value) {
        var promoCode = new PromoCode();
        promoCode.setCode("BIENVENUE10");
        promoCode.setType(PromoCodeType.PERCENT);
        promoCode.setValue(new BigDecimal(value));
        return promoCode;
    }

    private PromoCode amount(String value) {
        var promoCode = percent("1");
        promoCode.setType(PromoCodeType.AMOUNT);
        promoCode.setValue(new BigDecimal(value));
        return promoCode;
    }

    private PromoDiscounts.Evaluation evaluate(PromoCode code, String items, int uses) {
        return PromoDiscounts.evaluate(code, new BigDecimal(items), shipping, uses, now);
    }

    @Test
    void percentDiscountsTheJewelsOnly() {
        var evaluation = evaluate(percent("10"), "66.00", 0);

        assertTrue(evaluation.applies());
        assertEquals(new BigDecimal("6.60"), evaluation.discount());
        assertNull(evaluation.refusal());
    }

    @Test
    void percentIsRoundedToTheCent() {
        // 10 % de 33,35 € = 3,335 € : arrondi à 3,34 €.
        assertEquals(new BigDecimal("3.34"), evaluate(percent("10"), "33.35", 0).discount());
    }

    @Test
    void fixedAmountIsCappedToTheJewels() {
        assertEquals(new BigDecimal("15.00"), evaluate(amount("15"), "40.00", 0).discount());
        // Plafonnée aux bijoux et au minimum payable : 8 € de bijoux + 4,90 € de livraison − 0,50 € = 8,00 €.
        assertEquals(new BigDecimal("8.00"), evaluate(amount("50"), "8.00", 0).discount());
    }

    @Test
    void leavesTheMinimumPayableToStripe() {
        // 100 % de 10 € : le client paie la livraison moins 0,50 € de marge, jamais 0 €.
        var evaluation = evaluate(percent("100"), "10.00", 0);

        assertEquals(new BigDecimal("10.00"), evaluation.discount());
        var freeShipping = PromoDiscounts.evaluate(percent("100"), new BigDecimal("70.00"), BigDecimal.ZERO, 0, now);
        assertEquals(new BigDecimal("69.50"), freeShipping.discount());
    }

    @Test
    void refusesAnUnknownOrDeactivatedCodeWithTheSameMessage() {
        var deactivated = percent("10");
        deactivated.setActive(false);

        assertEquals("Ce code n'est pas valable.", evaluate(null, "40.00", 0).refusal());
        assertEquals("Ce code n'est pas valable.", evaluate(deactivated, "40.00", 0).refusal());
        assertFalse(evaluate(null, "40.00", 0).applies());
        assertEquals(0, evaluate(null, "40.00", 0).discount().signum());
    }

    @Test
    void refusesOutsideTheDates() {
        var expired = percent("10");
        expired.setEndsAt(Instant.parse("2026-11-14T10:00:00Z"));
        var notYet = percent("10");
        notYet.setStartsAt(Instant.parse("2026-11-14T10:00:01Z"));
        var current = percent("10");
        current.setStartsAt(now);
        current.setEndsAt(Instant.parse("2026-11-14T10:00:01Z"));

        assertEquals("Ce code a expiré.", evaluate(expired, "40.00", 0).refusal());
        assertEquals("Ce code n'est pas encore valable.", evaluate(notYet, "40.00", 0).refusal());
        assertTrue(evaluate(current, "40.00", 0).applies());
    }

    @Test
    void refusesWhenTheUsesAreExhausted() {
        var limited = percent("10");
        limited.setMaxUses(2);

        assertTrue(evaluate(limited, "40.00", 1).applies());
        assertEquals("Ce code n'est plus disponible.", evaluate(limited, "40.00", 2).refusal());
        assertEquals("Ce code n'est plus disponible.", evaluate(limited, "40.00", 3).refusal());
    }

    @Test
    void refusesBelowTheMinimumOfJewelsAndSaysHowMuch() {
        var withMinimum = percent("10");
        withMinimum.setMinimumAmount(new BigDecimal("60.00"));

        var refused = evaluate(withMinimum, "59.99", 0);

        assertEquals(0, refused.discount().signum());
        assertTrue(refused.refusal().contains("60,00"), refused.refusal());
        assertTrue(evaluate(withMinimum, "60.00", 0).applies());
    }

    @Test
    void refusesACodeThatWouldDiscountNothing() {
        // Panier trop bas : rien à remiser sans passer sous 0,50 €.
        var evaluation = PromoDiscounts.evaluate(percent("10"), new BigDecimal("0.30"), BigDecimal.ZERO, 0, now);

        assertFalse(evaluation.applies());
        assertEquals("Ce code ne s'applique pas à ce panier.", evaluation.refusal());
    }

}
