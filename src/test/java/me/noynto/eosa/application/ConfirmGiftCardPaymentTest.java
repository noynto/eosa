package me.noynto.eosa.application;

import me.noynto.eosa.checkout.Checkout;
import me.noynto.eosa.checkout.CheckoutProvider;
import me.noynto.eosa.checkout.CheckoutStatus;
import me.noynto.eosa.giftcard.GiftCard;
import me.noynto.eosa.giftcard.GiftCardMovementKind;
import me.noynto.eosa.giftcard.GiftCardProvider;
import me.noynto.eosa.giftcard.GiftCardState;
import me.noynto.eosa.mail.Mail;
import me.noynto.eosa.mail.MailProvider;
import me.noynto.eosa.shared.CheckoutId;
import me.noynto.eosa.shared.CheckoutSessionId;
import me.noynto.eosa.shared.GiftCardId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConfirmGiftCardPaymentTest {

    @Mock GiftCardProvider giftCardProvider;
    @Mock CheckoutProvider checkoutProvider;
    @Mock MailProvider mailProvider;

    private final Instant now = Instant.parse("2026-11-16T10:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final GiftCardId cardId = new GiftCardId("card-1");
    private final CheckoutSessionId sessionId = sessionId("cs_1");
    private ConfirmGiftCardPayment useCase;
    private GiftCard card;
    private Checkout checkout;

    private static CheckoutSessionId sessionId(String value) {
        var id = new CheckoutSessionId();
        id.setStripe(value);
        return id;
    }

    @BeforeEach
    void setUp() {
        useCase = new ConfirmGiftCardPayment(giftCardProvider, checkoutProvider, mailProvider, clock, 365,
                URI.create("https://eosa.test"));
        card = new GiftCard();
        card.setId(cardId);
        card.setCode("ABCDEFGH2345");
        card.setToken("token");
        card.setState(GiftCardState.PENDING_PAYMENT);
        card.setInitialAmount(new BigDecimal("50.00"));
        card.setBuyerFirstName("Léa");
        card.setBuyerEmail("lea@example.com");
        card.setStripeSessionId("cs_1");
        card.setExpiresAt(now);
        checkout = new Checkout();
        var checkoutId = new CheckoutId();
        checkoutId.setStripe("cs_1");
        checkout.setId(checkoutId);
        checkout.setGiftCardId(cardId);
        checkout.setStatus(CheckoutStatus.COMPLETED);
        checkout.setTotalAmount(new BigDecimal("50.00"));
        when(checkoutProvider.readIds(any(), any(), any())).thenAnswer(inv -> Stream.of(checkoutId));
        when(checkoutProvider.read(checkoutId)).thenReturn(Optional.of(checkout));
        when(giftCardProvider.read(cardId)).thenAnswer(inv -> Optional.of(card));
        when(giftCardProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void handle_activatesThePaidCardWritesItsFirstMovementAndSendsTheEmail() {
        var result = useCase.handle(new ConfirmGiftCardPayment.Command(sessionId));

        assertTrue(result.paid());
        assertEquals(GiftCardState.ACTIVE, card.getState());
        assertEquals(1, card.getMovements().size());
        assertEquals(GiftCardMovementKind.ISSUED, card.getMovements().getFirst().getKind());
        assertEquals(0, new BigDecimal("50.00").compareTo(card.balance()));
        // Validité d'un an depuis le paiement : jusqu'au 16 novembre 2027 inclus.
        assertEquals(Instant.parse("2027-11-16T23:00:00Z"), card.getExpiresAt());
        var mail = ArgumentCaptor.forClass(Mail.class);
        verify(mailProvider).send(mail.capture());
        assertEquals("lea@example.com", mail.getValue().to());
        assertEquals("gift-card", mail.getValue().template());
        assertNotNull(card.getEmailSentAt());
    }

    @Test
    void handle_isIdempotent() {
        useCase.handle(new ConfirmGiftCardPayment.Command(sessionId));
        useCase.handle(new ConfirmGiftCardPayment.Command(sessionId));

        assertEquals(1, card.getMovements().size());
        verify(mailProvider, org.mockito.Mockito.times(1)).send(any());
    }

    @Test
    void handle_doesNothingWhileStripeHasNotConfirmed() {
        checkout.setStatus(CheckoutStatus.PENDING);

        var result = useCase.handle(new ConfirmGiftCardPayment.Command(sessionId));

        assertFalse(result.paid());
        assertEquals(GiftCardState.PENDING_PAYMENT, card.getState());
        assertTrue(card.getMovements().isEmpty());
        verify(mailProvider, never()).send(any());
    }

    @Test
    void handle_keepsTheCardActiveAndRetriesTheEmailLaterWhenItDoesNotLeave() {
        doThrow(new RuntimeException("SMTP")).when(mailProvider).send(any());

        var result = useCase.handle(new ConfirmGiftCardPayment.Command(sessionId));

        assertTrue(result.paid());
        assertEquals(GiftCardState.ACTIVE, card.getState());
        assertEquals(null, card.getEmailSentAt());
    }

    @Test
    void handle_refusesASessionThatIsNotTheOneOfTheCard() {
        card.setStripeSessionId("cs_other");

        assertThrows(RuntimeException.class, () -> useCase.handle(new ConfirmGiftCardPayment.Command(sessionId)));
        assertEquals(GiftCardState.PENDING_PAYMENT, card.getState());
    }

    @Test
    void handle_refusesAnAmountThatDoesNotMatchTheCard() {
        checkout.setTotalAmount(new BigDecimal("10.00"));

        assertThrows(RuntimeException.class, () -> useCase.handle(new ConfirmGiftCardPayment.Command(sessionId)));
        assertEquals(GiftCardState.PENDING_PAYMENT, card.getState());
    }

    @Test
    void handle_refusesASessionThatBuysNoCard() {
        checkout.setGiftCardId(null);

        assertThrows(RuntimeException.class, () -> useCase.handle(new ConfirmGiftCardPayment.Command(sessionId)));
    }

    @Test
    void handle_refusesAMissingSessionId() {
        assertThrows(RuntimeException.class, () -> useCase.handle(new ConfirmGiftCardPayment.Command(null)));
        assertThrows(RuntimeException.class, () -> useCase.handle(new ConfirmGiftCardPayment.Command(sessionId(" "))));
    }

}
