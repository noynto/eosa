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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestoreGiftCardBalanceTest {

    @Mock GiftCardProvider giftCardProvider;

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"), ZoneOffset.UTC);
    private final GiftCardId id = new GiftCardId("card");

    private GiftCard card(GiftCardState state, Instant expiresAt) {
        var giftCard = new GiftCard();
        giftCard.setId(id);
        giftCard.setState(state);
        giftCard.setExpiresAt(expiresAt);
        giftCard.getMovements().add(new GiftCardMovement(GiftCardMovementKind.ISSUED, new BigDecimal("50.00"), null, clock.instant()));
        giftCard.getMovements().add(new GiftCardMovement(GiftCardMovementKind.RESERVED, new BigDecimal("30.00"), null, clock.instant()));
        giftCard.getMovements().add(new GiftCardMovement(GiftCardMovementKind.USED, new BigDecimal("30.00"), null, clock.instant()));
        return giftCard;
    }

    private RestoreGiftCardBalance.Command command(String amount, String reason) {
        return new RestoreGiftCardBalance.Command(id, amount == null ? null : new BigDecimal(amount), reason);
    }

    @Test
    void handle_addsARestoredMovementWithItsReason() {
        when(giftCardProvider.read(id)).thenReturn(Optional.of(card(GiftCardState.ACTIVE, clock.instant().plusSeconds(60))));
        when(giftCardProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var giftCard = new RestoreGiftCardBalance(giftCardProvider, clock)
                .handle(command("12.5", " Commande annulée ")).giftCard();

        var last = giftCard.getMovements().getLast();
        assertEquals(GiftCardMovementKind.RESTORED, last.getKind());
        assertEquals(new BigDecimal("12.50"), last.getAmount());
        assertEquals("Commande annulée", last.getReason());
        assertEquals(new BigDecimal("32.50"), giftCard.balance());
    }

    @Test
    void handle_rejectsAnInvalidAmountOrAMissingReason() {
        var useCase = new RestoreGiftCardBalance(giftCardProvider, clock);

        assertThrows(RuntimeException.class, () -> useCase.handle(command(null, "Motif")));
        assertThrows(RuntimeException.class, () -> useCase.handle(command("0", "Motif")));
        assertThrows(RuntimeException.class, () -> useCase.handle(command("-1", "Motif")));
        assertThrows(RuntimeException.class, () -> useCase.handle(command("1000.01", "Motif")));
        assertThrows(RuntimeException.class, () -> useCase.handle(command("10", " ")));
        verify(giftCardProvider, never()).write(any());
    }

    @Test
    void handle_refusesAVoidOrExpiredCard() {
        var useCase = new RestoreGiftCardBalance(giftCardProvider, clock);

        when(giftCardProvider.read(id)).thenReturn(Optional.of(card(GiftCardState.VOID, clock.instant().plusSeconds(60))));
        assertThrows(RuntimeException.class, () -> useCase.handle(command("10", "Motif")));

        when(giftCardProvider.read(id)).thenReturn(Optional.of(card(GiftCardState.ACTIVE, clock.instant())));
        assertThrows(RuntimeException.class, () -> useCase.handle(command("10", "Motif")));
        verify(giftCardProvider, never()).write(any());
    }

}
