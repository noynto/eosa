package me.noynto.eosa.giftcard;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GiftCardTest {

    private final Instant now = Instant.parse("2026-11-16T10:00:00Z");

    private GiftCard card(GiftCardState state, Instant expiresAt, GiftCardMovement... movements) {
        var giftCard = new GiftCard();
        giftCard.setState(state);
        giftCard.setExpiresAt(expiresAt);
        giftCard.getMovements().addAll(java.util.List.of(movements));
        return giftCard;
    }

    private GiftCardMovement movement(GiftCardMovementKind kind, String amount) {
        return new GiftCardMovement(kind, new BigDecimal(amount), null, now);
    }

    @Test
    void balance_addsIssuedReleasedAndRestoredAndSubtractsReservedAndVoided() {
        var giftCard = card(GiftCardState.ACTIVE, now.plusSeconds(60),
                movement(GiftCardMovementKind.ISSUED, "50.00"),
                movement(GiftCardMovementKind.RESERVED, "30.00"),
                movement(GiftCardMovementKind.USED, "30.00"),
                movement(GiftCardMovementKind.RESERVED, "10.00"),
                movement(GiftCardMovementKind.RELEASED, "10.00"),
                movement(GiftCardMovementKind.RESTORED, "5.00"));

        assertEquals(0, new BigDecimal("25.00").compareTo(giftCard.balance()));
    }

    @Test
    void balance_isZeroAfterVoid() {
        var giftCard = card(GiftCardState.VOID, now.plusSeconds(60),
                movement(GiftCardMovementKind.ISSUED, "50.00"),
                movement(GiftCardMovementKind.VOIDED, "50.00"));

        assertEquals(0, BigDecimal.ZERO.compareTo(giftCard.balance()));
    }

    @Test
    void isExpired_endIsExclusive() {
        assertFalse(card(GiftCardState.ACTIVE, now.plusSeconds(1)).isExpired(now));
        assertTrue(card(GiftCardState.ACTIVE, now).isExpired(now));
    }

    @Test
    void isUsable_requiresActiveNotExpiredAndPositiveBalance() {
        var issued = movement(GiftCardMovementKind.ISSUED, "20.00");

        assertTrue(card(GiftCardState.ACTIVE, now.plusSeconds(1), issued).isUsable(now));
        assertFalse(card(GiftCardState.PENDING_PAYMENT, now.plusSeconds(1), issued).isUsable(now));
        assertFalse(card(GiftCardState.VOID, now.plusSeconds(1), issued).isUsable(now));
        assertFalse(card(GiftCardState.ACTIVE, now, issued).isUsable(now));
        assertFalse(card(GiftCardState.ACTIVE, now.plusSeconds(1)).isUsable(now));
    }

}
