package me.noynto.eosa.service;

import me.noynto.eosa.giftcard.GiftCard;
import me.noynto.eosa.giftcard.GiftCardMovement;
import me.noynto.eosa.giftcard.GiftCardMovementKind;
import me.noynto.eosa.giftcard.GiftCardState;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GiftCardDeductionsTest {

    private final Instant now = Instant.parse("2026-11-16T10:00:00Z");

    private GiftCard card(String balance) {
        var giftCard = new GiftCard();
        giftCard.setState(GiftCardState.ACTIVE);
        giftCard.setExpiresAt(now.plusSeconds(3600));
        giftCard.getMovements().add(new GiftCardMovement(GiftCardMovementKind.ISSUED, new BigDecimal(balance), null, now));
        return giftCard;
    }

    private GiftCardDeductions.Evaluation evaluate(GiftCard card, String items, String shipping) {
        return GiftCardDeductions.evaluate(card, new BigDecimal(items), new BigDecimal(shipping), now);
    }

    @Test
    void paysTheBalanceWhenItIsBelowTheJewels() {
        var evaluation = evaluate(card("20"), "42", "5");

        assertTrue(evaluation.applies());
        assertEquals(new BigDecimal("20.00"), evaluation.deduction());
    }

    @Test
    void paysTheJewelsOnlyNeverTheShipping() {
        var evaluation = evaluate(card("100"), "42", "5");

        assertEquals(new BigDecimal("42.00"), evaluation.deduction());
    }

    @Test
    void coversEverythingWhenTheShippingIsFree() {
        var evaluation = evaluate(card("100"), "70", "0");

        assertEquals(new BigDecimal("70.00"), evaluation.deduction());
    }

    @Test
    void leavesAtLeastFiftyCentsWhenTheCardWouldLeaveLess() {
        // 70,20 € de bijoux, livraison offerte, solde de 70 € : il resterait 0,20 €, Stripe refuse (0, 0,50 €).
        var evaluation = evaluate(card("70"), "70.20", "0");

        assertEquals(new BigDecimal("69.70"), evaluation.deduction());
    }

    @Test
    void keepsTheRemainderWhenItIsAtLeastFiftyCents() {
        var evaluation = evaluate(card("70"), "70.50", "0");

        assertEquals(new BigDecimal("70.00"), evaluation.deduction());
    }

    @Test
    void refusesAnUnknownExpiredVoidOrEmptyCardWithTheSameMessage() {
        var expired = card("50");
        expired.setExpiresAt(now);
        var voided = card("50");
        voided.setState(GiftCardState.VOID);
        var empty = card("50");
        empty.getMovements().add(new GiftCardMovement(GiftCardMovementKind.RESERVED, new BigDecimal("50"), null, now));

        for (var evaluation : new GiftCardDeductions.Evaluation[]{
                evaluate(null, "42", "5"), evaluate(expired, "42", "5"), evaluate(voided, "42", "5"), evaluate(empty, "42", "5")}) {
            assertFalse(evaluation.applies());
            assertEquals("Ce code n'est pas valable.", evaluation.refusal());
            assertEquals(0, BigDecimal.ZERO.compareTo(evaluation.deduction()));
        }
    }

}
