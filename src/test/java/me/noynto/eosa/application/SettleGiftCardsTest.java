package me.noynto.eosa.application;

import me.noynto.eosa.checkout.Checkout;
import me.noynto.eosa.checkout.CheckoutProvider;
import me.noynto.eosa.checkout.CheckoutStatus;
import me.noynto.eosa.giftcard.GiftCard;
import me.noynto.eosa.giftcard.GiftCardMovement;
import me.noynto.eosa.giftcard.GiftCardMovementKind;
import me.noynto.eosa.giftcard.GiftCardProvider;
import me.noynto.eosa.giftcard.GiftCardState;
import me.noynto.eosa.mail.MailProvider;
import me.noynto.eosa.shared.CheckoutId;
import me.noynto.eosa.shared.GiftCardId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SettleGiftCardsTest {

    @Mock GiftCardProvider giftCardProvider;
    @Mock CheckoutProvider checkoutProvider;
    @Mock MailProvider mailProvider;

    private final Instant now = Instant.parse("2026-11-16T10:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final GiftCardId cardId = new GiftCardId("card-1");
    private SettleGiftCards useCase;
    private GiftCard card;

    @BeforeEach
    void setUp() {
        useCase = new SettleGiftCards(giftCardProvider, checkoutProvider, mailProvider, clock, 365,
                URI.create("https://eosa.test"));
        card = new GiftCard();
        card.setId(cardId);
        card.setCode("ABCDEFGH2345");
        card.setToken("token");
        card.setInitialAmount(new BigDecimal("50.00"));
        card.setBuyerEmail("lea@example.com");
        card.setStripeSessionId("cs_1");
        card.setExpiresAt(now);
        card.setCreatedAt(now.minus(Duration.ofMinutes(20)));
        when(giftCardProvider.readIds(any(), any())).thenAnswer(inv -> Stream.of(cardId));
        when(giftCardProvider.read(cardId)).thenAnswer(inv -> Optional.of(card));
        when(giftCardProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Checkout stripeSession(CheckoutStatus status, String total) {
        var checkout = new Checkout();
        var id = new CheckoutId();
        id.setStripe("cs_1");
        checkout.setId(id);
        checkout.setGiftCardId(cardId);
        checkout.setStatus(status);
        checkout.setTotalAmount(new BigDecimal(total));
        when(checkoutProvider.readIds(any(), any(), any())).thenAnswer(inv -> Stream.of(id));
        when(checkoutProvider.read(id)).thenReturn(Optional.of(checkout));
        return checkout;
    }

    private void active(Instant issuedAt, Instant emailSentAt) {
        card.setState(GiftCardState.ACTIVE);
        card.setEmailSentAt(emailSentAt);
        card.getMovements().add(new GiftCardMovement(GiftCardMovementKind.ISSUED, card.getInitialAmount(), null, issuedAt));
    }

    @Test
    void handle_activatesAPaidPendingCardAndEmailsItRightAway() {
        card.setState(GiftCardState.PENDING_PAYMENT);
        stripeSession(CheckoutStatus.COMPLETED, "50.00");

        var result = useCase.handle();

        assertEquals(new SettleGiftCards.Result(1, 1, 0, 0, 0), result);
        assertEquals(GiftCardState.ACTIVE, card.getState());
        assertEquals(1, card.getMovements().size());
        assertEquals(Instant.parse("2027-11-16T23:00:00Z"), card.getExpiresAt());
        assertNotNull(card.getEmailSentAt());
        verify(mailProvider).send(any());
    }

    @Test
    void handle_leavesAYoungUnpaidCardAlone() {
        card.setState(GiftCardState.PENDING_PAYMENT);
        stripeSession(CheckoutStatus.PENDING, "50.00");

        var result = useCase.handle();

        assertEquals(new SettleGiftCards.Result(0, 0, 0, 0, 0), result);
        verify(giftCardProvider, never()).delete(any());
        verify(mailProvider, never()).send(any());
    }

    @Test
    void handle_deletesAnUnpaidCardOlderThanADay() {
        card.setState(GiftCardState.PENDING_PAYMENT);
        card.setCreatedAt(now.minus(Duration.ofHours(25)));
        stripeSession(CheckoutStatus.PENDING, "50.00");

        var result = useCase.handle();

        assertEquals(1, result.deleted());
        verify(giftCardProvider).delete(cardId);
    }

    @Test
    void handle_activatesAPaidCardEvenWhenItIsOlderThanADay() {
        card.setState(GiftCardState.PENDING_PAYMENT);
        card.setCreatedAt(now.minus(Duration.ofHours(30)));
        stripeSession(CheckoutStatus.COMPLETED, "50.00");

        var result = useCase.handle();

        assertEquals(1, result.activated());
        verify(giftCardProvider, never()).delete(any());
    }

    @Test
    void handle_neverDeletesWhenStripeDoesNotReturnTheSession() {
        card.setState(GiftCardState.PENDING_PAYMENT);
        card.setCreatedAt(now.minus(Duration.ofHours(30)));
        when(checkoutProvider.readIds(any(), any(), any())).thenAnswer(inv -> Stream.empty());

        var result = useCase.handle();

        assertEquals(1, result.failed());
        verify(giftCardProvider, never()).delete(any());
    }

    @Test
    void handle_deletesAnOldCardThatNeverReachedStripe() {
        card.setState(GiftCardState.PENDING_PAYMENT);
        card.setStripeSessionId(null);
        card.setCreatedAt(now.minus(Duration.ofHours(25)));

        assertEquals(1, useCase.handle().deleted());
        verify(giftCardProvider).delete(cardId);
    }

    @Test
    void handle_refusesToActivateWhenThePaidAmountDiffers() {
        card.setState(GiftCardState.PENDING_PAYMENT);
        stripeSession(CheckoutStatus.COMPLETED, "10.00");

        var result = useCase.handle();

        assertEquals(1, result.failed());
        assertEquals(GiftCardState.PENDING_PAYMENT, card.getState());
    }

    @Test
    void handle_resendsTheEmailThatNeverLeftOnceTheGracePeriodIsOver() {
        active(now.minus(Duration.ofMinutes(6)), null);

        var result = useCase.handle();

        assertEquals(1, result.emailsSent());
        assertNotNull(card.getEmailSentAt());
    }

    @Test
    void handle_waitsForTheGracePeriodAndNeverSendsTwice() {
        active(now.minus(Duration.ofMinutes(2)), null);
        useCase.handle();
        verify(mailProvider, never()).send(any());

        card.getMovements().clear();
        active(now.minus(Duration.ofHours(1)), now.minus(Duration.ofMinutes(50)));
        useCase.handle();
        verify(mailProvider, never()).send(any());
    }

    @Test
    void handle_stopsResendingAfterAWeek() {
        active(now.minus(Duration.ofDays(8)), null);

        useCase.handle();

        verify(mailProvider, never()).send(any());
    }

    @Test
    void handle_countsAnEmailFailureAndKeepsItForTheNextPass() {
        active(now.minus(Duration.ofMinutes(6)), null);
        doThrow(new RuntimeException("SMTP")).when(mailProvider).send(any());

        var result = useCase.handle();

        assertEquals(1, result.failed());
        assertNull(card.getEmailSentAt());
    }

    private void reserved(Instant at) {
        active(now.minus(Duration.ofDays(1)), now.minus(Duration.ofDays(1)));
        var reservation = card.reserve(new BigDecimal("20.00"), at);
        reservation.setCheckoutSessionId("cs_1");
    }

    @Test
    void handle_releasesAReservationWhoseSessionWasNeverPaidOnceItIsOldEnough() {
        reserved(now.minus(Duration.ofMinutes(41)));
        stripeSession(CheckoutStatus.PENDING, "30.00");

        var result = useCase.handle();

        assertEquals(1, result.released());
        assertEquals(0, new BigDecimal("50.00").compareTo(card.balance()));
        assertEquals(0, card.openReservations().size());
    }

    @Test
    void handle_keepsAYoungReservationAndNeverReleasesAPaidSession() {
        reserved(now.minus(Duration.ofMinutes(10)));
        useCase.handle();
        assertEquals(1, card.openReservations().size());

        card.getMovements().clear();
        reserved(now.minus(Duration.ofMinutes(60)));
        stripeSession(CheckoutStatus.COMPLETED, "30.00");
        var result = useCase.handle();

        assertEquals(0, result.released());
        assertEquals(1, card.openReservations().size(), "le rattrapage des commandes en fera une utilisation");
    }

    @Test
    void handle_releasesNothingWhenStripeDoesNotReturnTheSession() {
        reserved(now.minus(Duration.ofMinutes(60)));
        when(checkoutProvider.readIds(any(), any(), any())).thenAnswer(inv -> Stream.empty());

        var result = useCase.handle();

        assertEquals(1, result.failed());
        assertEquals(1, card.openReservations().size());
    }

    @Test
    void handle_ignoresAVoidCard() {
        card.setState(GiftCardState.VOID);
        card.getMovements().add(new GiftCardMovement(GiftCardMovementKind.ISSUED, card.getInitialAmount(), null, now.minus(Duration.ofHours(1))));

        assertEquals(new SettleGiftCards.Result(0, 0, 0, 0, 0), useCase.handle());
        verify(mailProvider, never()).send(any());
        verify(giftCardProvider, times(0)).write(any());
    }

}
