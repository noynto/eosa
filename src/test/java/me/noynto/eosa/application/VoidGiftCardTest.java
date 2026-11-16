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
class VoidGiftCardTest {

    @Mock GiftCardProvider giftCardProvider;

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"), ZoneOffset.UTC);
    private final GiftCardId id = new GiftCardId("card");

    private GiftCard card(GiftCardState state, String issued, String used) {
        var giftCard = new GiftCard();
        giftCard.setId(id);
        giftCard.setState(state);
        giftCard.getMovements().add(new GiftCardMovement(GiftCardMovementKind.ISSUED, new BigDecimal(issued), null, clock.instant()));
        if (used != null) {
            giftCard.getMovements().add(new GiftCardMovement(GiftCardMovementKind.RESERVED, new BigDecimal(used), null, clock.instant()));
            giftCard.getMovements().add(new GiftCardMovement(GiftCardMovementKind.USED, new BigDecimal(used), null, clock.instant()));
        }
        return giftCard;
    }

    @Test
    void handle_voidsTheCardAndRemovesTheRemainingBalance() {
        when(giftCardProvider.read(id)).thenReturn(Optional.of(card(GiftCardState.ACTIVE, "50.00", "30.00")));
        when(giftCardProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = new VoidGiftCard(giftCardProvider, clock).handle(new VoidGiftCard.Command(id, " Perdue "));

        var giftCard = result.giftCard();
        assertEquals(GiftCardState.VOID, giftCard.getState());
        assertEquals(0, BigDecimal.ZERO.compareTo(giftCard.balance()));
        var last = giftCard.getMovements().getLast();
        assertEquals(GiftCardMovementKind.VOIDED, last.getKind());
        assertEquals(new BigDecimal("20.00"), last.getAmount());
        assertEquals("Perdue", last.getReason());
    }

    @Test
    void handle_doesNotWriteAMovementWhenTheBalanceIsEmpty() {
        when(giftCardProvider.read(id)).thenReturn(Optional.of(card(GiftCardState.ACTIVE, "50.00", "50.00")));
        when(giftCardProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var giftCard = new VoidGiftCard(giftCardProvider, clock).handle(new VoidGiftCard.Command(id, null)).giftCard();

        assertEquals(GiftCardState.VOID, giftCard.getState());
        assertEquals(3, giftCard.getMovements().size());
    }

    @Test
    void handle_refusesACardThatIsNotActive() {
        when(giftCardProvider.read(id)).thenReturn(Optional.of(card(GiftCardState.VOID, "50.00", null)));
        var useCase = new VoidGiftCard(giftCardProvider, clock);

        assertThrows(RuntimeException.class, () -> useCase.handle(new VoidGiftCard.Command(id, null)));
        verify(giftCardProvider, never()).write(any());
    }

    @Test
    void handle_throwsWhenIdIsMissingOrCardUnknown() {
        var useCase = new VoidGiftCard(giftCardProvider, clock);
        when(giftCardProvider.read(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> useCase.handle(new VoidGiftCard.Command(null, null)));
        assertThrows(RuntimeException.class, () -> useCase.handle(new VoidGiftCard.Command(id, null)));
    }

}
