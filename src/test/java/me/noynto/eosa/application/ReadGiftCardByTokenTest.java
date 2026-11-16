package me.noynto.eosa.application;

import me.noynto.eosa.giftcard.GiftCard;
import me.noynto.eosa.giftcard.GiftCardMovement;
import me.noynto.eosa.giftcard.GiftCardMovementKind;
import me.noynto.eosa.giftcard.GiftCardProvider;
import me.noynto.eosa.giftcard.GiftCardState;
import me.noynto.eosa.shared.GiftCardId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadGiftCardByTokenTest {

    @Mock GiftCardProvider giftCardProvider;

    private final Instant now = Instant.parse("2026-11-16T10:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final GiftCardId id = new GiftCardId("card");

    private void stub(GiftCardState state, Instant expiresAt) {
        var giftCard = new GiftCard();
        giftCard.setId(id);
        giftCard.setState(state);
        giftCard.setExpiresAt(expiresAt);
        giftCard.getMovements().add(new GiftCardMovement(GiftCardMovementKind.ISSUED, new BigDecimal("50.00"), null, now));
        when(giftCardProvider.readIds(isNull(), eq("tok"))).thenReturn(Stream.of(id));
        when(giftCardProvider.read(id)).thenReturn(Optional.of(giftCard));
    }

    @Test
    void handle_readsAnActiveCardByItsToken() {
        stub(GiftCardState.ACTIVE, now.plusSeconds(60));

        var result = new ReadGiftCardByToken(giftCardProvider, clock).handle(new ReadGiftCardByToken.Command("tok"));

        assertEquals(id, result.giftCard().getId());
        assertTrue(result.usable());
    }

    @Test
    void handle_stillShowsAnExpiredCardButNotAsUsable() {
        stub(GiftCardState.ACTIVE, now);

        assertFalse(new ReadGiftCardByToken(giftCardProvider, clock).handle(new ReadGiftCardByToken.Command("tok")).usable());
    }

    @Test
    void handle_hidesACardStillWaitingForItsPayment() {
        stub(GiftCardState.PENDING_PAYMENT, now.plusSeconds(60));
        var useCase = new ReadGiftCardByToken(giftCardProvider, clock);

        assertThrows(RuntimeException.class, () -> useCase.handle(new ReadGiftCardByToken.Command("tok")));
    }

    @Test
    void handle_failsForAnUnknownOrMissingToken() {
        when(giftCardProvider.readIds(isNull(), eq("nope"))).thenReturn(Stream.empty());
        var useCase = new ReadGiftCardByToken(giftCardProvider, clock);

        assertThrows(RuntimeException.class, () -> useCase.handle(new ReadGiftCardByToken.Command("nope")));
        assertThrows(RuntimeException.class, () -> useCase.handle(new ReadGiftCardByToken.Command(null)));
        assertThrows(RuntimeException.class, () -> useCase.handle(new ReadGiftCardByToken.Command(" ")));
    }

}
