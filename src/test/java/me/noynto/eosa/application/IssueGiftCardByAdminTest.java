package me.noynto.eosa.application;

import me.noynto.eosa.giftcard.GiftCardMovementKind;
import me.noynto.eosa.giftcard.GiftCardProvider;
import me.noynto.eosa.giftcard.GiftCardState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueGiftCardByAdminTest {

    @Mock GiftCardProvider giftCardProvider;

    // 16 novembre 2026, 11 h à Paris.
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"), ZoneOffset.UTC);
    private IssueGiftCardByAdmin useCase;

    @BeforeEach
    void setUp() {
        useCase = new IssueGiftCardByAdmin(giftCardProvider, clock, 365);
    }

    private void stubWrite() {
        lenient().when(giftCardProvider.readIds(anyString(), isNull())).thenAnswer(inv -> Stream.empty());
        lenient().when(giftCardProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private IssueGiftCardByAdmin.Command command(String amount, String email, String reason) {
        return new IssueGiftCardByAdmin.Command(amount == null ? null : new BigDecimal(amount), "Léa", email, reason);
    }

    @Test
    void handle_issuesAnActiveCardWithItsFirstMovement() {
        stubWrite();

        var card = useCase.handle(command("50", " lea@example.com ", " Lot du concours ")).giftCard();

        assertEquals(GiftCardState.ACTIVE, card.getState());
        assertEquals(new BigDecimal("50.00"), card.getInitialAmount());
        assertEquals(new BigDecimal("50.00"), card.balance());
        assertEquals("lea@example.com", card.getBuyerEmail());
        assertEquals(1, card.getMovements().size());
        assertEquals(GiftCardMovementKind.ISSUED, card.getMovements().getFirst().getKind());
        assertEquals("Lot du concours", card.getMovements().getFirst().getReason());
        assertEquals(12, card.getCode().length());
        assertTrue(card.getCode().chars().allMatch(c -> me.noynto.eosa.service.GiftCardIssuing.CODE_ALPHABET.indexOf(c) >= 0));
        assertTrue(card.getToken().length() >= 43);
    }

    @Test
    void handle_validityLastDayIsIncluded() {
        stubWrite();

        var card = useCase.handle(command("50", "lea@example.com", "Cadeau")).giftCard();

        // Valable jusqu'au 16 novembre 2027 inclus : fin exclusive au 17 novembre 2027 à minuit, heure de Paris.
        assertEquals(Instant.parse("2027-11-16T23:00:00Z"), card.getExpiresAt());
    }

    @Test
    void handle_generatesDistinctCodesAndTokens() {
        stubWrite();

        var first = useCase.handle(command("10", "a@example.com", "Cadeau")).giftCard();
        var second = useCase.handle(command("10", "a@example.com", "Cadeau")).giftCard();

        assertNotEquals(first.getCode(), second.getCode());
        assertNotEquals(first.getToken(), second.getToken());
    }

    @Test
    void handle_retriesWhenTheCodeIsTaken() {
        when(giftCardProvider.readIds(anyString(), isNull()))
                .thenReturn(Stream.of(new me.noynto.eosa.shared.GiftCardId("x")))
                .thenReturn(Stream.empty());
        when(giftCardProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.handle(command("10", "a@example.com", "Cadeau"));

        verify(giftCardProvider).write(any());
    }

    @Test
    void handle_rejectsInvalidInput() {
        assertThrows(RuntimeException.class, () -> useCase.handle(command(null, "a@example.com", "Cadeau")));
        assertThrows(RuntimeException.class, () -> useCase.handle(command("0", "a@example.com", "Cadeau")));
        assertThrows(RuntimeException.class, () -> useCase.handle(command("-5", "a@example.com", "Cadeau")));
        assertThrows(RuntimeException.class, () -> useCase.handle(command("1000.01", "a@example.com", "Cadeau")));
        assertThrows(RuntimeException.class, () -> useCase.handle(command("50", "pas-un-email", "Cadeau")));
        assertThrows(RuntimeException.class, () -> useCase.handle(command("50", null, "Cadeau")));
        assertThrows(RuntimeException.class, () -> useCase.handle(command("50", "a@example.com", "  ")));
        verify(giftCardProvider, never()).write(any());
    }

}
