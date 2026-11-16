package me.noynto.eosa.application;

import me.noynto.eosa.checkout.Checkout;
import me.noynto.eosa.checkout.CheckoutProvider;
import me.noynto.eosa.checkout.CheckoutSession;
import me.noynto.eosa.giftcard.GiftCard;
import me.noynto.eosa.giftcard.GiftCardProvider;
import me.noynto.eosa.giftcard.GiftCardState;
import me.noynto.eosa.shared.CheckoutSessionId;
import me.noynto.eosa.shared.GiftCardId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
class PurchaseGiftCardTest {

    @Mock GiftCardProvider giftCardProvider;
    @Mock CheckoutProvider checkoutProvider;

    private final Instant now = Instant.parse("2026-11-16T10:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private PurchaseGiftCard useCase;

    @BeforeEach
    void setUp() {
        useCase = new PurchaseGiftCard(giftCardProvider, checkoutProvider, clock, 365,
                new BigDecimal("10"), new BigDecimal("200"));
        lenient().when(giftCardProvider.readIds(anyString(), isNull())).thenAnswer(inv -> Stream.empty());
        lenient().when(giftCardProvider.write(any())).thenAnswer(inv -> {
            GiftCard giftCard = inv.getArgument(0);
            if (giftCard.getId() == null) {
                giftCard.setId(new GiftCardId("card-1"));
            }
            return giftCard;
        });
        lenient().when(checkoutProvider.write(any())).thenAnswer(inv -> {
            Checkout checkout = inv.getArgument(0);
            var session = new CheckoutSession();
            var id = new CheckoutSessionId();
            id.setStripe("cs_1");
            session.setId(id);
            session.setUri(URI.create("https://stripe.test/pay"));
            checkout.setSession(session);
            return checkout;
        });
    }

    private PurchaseGiftCard.Command command(String amount) {
        return new PurchaseGiftCard.Command(amount == null ? null : new BigDecimal(amount), " Léa ", " lea@example.com ");
    }

    @Test
    void handle_createsAPendingCardAndASessionThatExpiresAfterHalfAnHour() {
        var result = useCase.handle(command("50"));

        assertEquals(URI.create("https://stripe.test/pay"), result.paymentUri());
        var checkout = ArgumentCaptor.forClass(Checkout.class);
        verify(checkoutProvider).write(checkout.capture());
        assertEquals(new GiftCardId("card-1"), checkout.getValue().getGiftCardId());
        assertNull(checkout.getValue().getCartId(), "pas de panier : le rattrapage des commandes l'ignore");
        assertEquals(1, checkout.getValue().getItems().size());
        assertEquals(new BigDecimal("50.00"), checkout.getValue().getItems().getFirst().getUnitPrice());
        assertEquals("lea@example.com", checkout.getValue().getCustomerEmail());
        assertTrue(checkout.getValue().getExpiresAt().isAfter(now.plusSeconds(30 * 60)), "au moins 30 minutes pour Stripe");

        var card = ArgumentCaptor.forClass(GiftCard.class);
        verify(giftCardProvider, org.mockito.Mockito.times(2)).write(card.capture());
        var written = card.getValue();
        assertEquals(GiftCardState.PENDING_PAYMENT, written.getState());
        assertEquals("cs_1", written.getStripeSessionId());
        assertEquals("Léa", written.getBuyerFirstName());
        assertTrue(written.getMovements().isEmpty(), "rien dans le journal avant le paiement");
    }

    @Test
    void handle_acceptsTheBoundsAndRefusesWhatIsOutside() {
        useCase.handle(command("10"));
        useCase.handle(command("200"));

        assertThrows(RuntimeException.class, () -> useCase.handle(command("9")));
        assertThrows(RuntimeException.class, () -> useCase.handle(command("201")));
        assertThrows(RuntimeException.class, () -> useCase.handle(command("12.5")));
        assertThrows(RuntimeException.class, () -> useCase.handle(command(null)));
    }

    @Test
    void handle_refusesAMissingNameOrAnInvalidEmail() {
        assertThrows(RuntimeException.class, () -> useCase.handle(
                new PurchaseGiftCard.Command(new BigDecimal("50"), " ", "lea@example.com")));
        assertThrows(RuntimeException.class, () -> useCase.handle(
                new PurchaseGiftCard.Command(new BigDecimal("50"), "Léa", "pas-un-email")));
        verify(checkoutProvider, never()).write(any());
    }

    @Test
    void handle_deletesThePendingCardWhenStripeRefuses() {
        org.mockito.Mockito.doThrow(new RuntimeException("Stripe indisponible")).when(checkoutProvider).write(any());

        assertThrows(RuntimeException.class, () -> useCase.handle(command("50")));

        verify(giftCardProvider).delete(new GiftCardId("card-1"));
    }

}
