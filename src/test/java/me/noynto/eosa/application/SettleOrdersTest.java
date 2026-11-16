package me.noynto.eosa.application;

import me.noynto.eosa.cart.CartProvider;
import me.noynto.eosa.checkout.Checkout;
import me.noynto.eosa.checkout.CheckoutItem;
import me.noynto.eosa.checkout.CheckoutProvider;
import me.noynto.eosa.checkout.CheckoutSession;
import me.noynto.eosa.checkout.CheckoutStatus;
import me.noynto.eosa.mail.Mail;
import me.noynto.eosa.mail.MailProvider;
import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.shared.CartId;
import me.noynto.eosa.shared.CheckoutId;
import me.noynto.eosa.shared.CheckoutSessionId;
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
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SettleOrdersTest {

    @Mock CheckoutProvider checkoutProvider;
    @Mock CartProvider cartProvider;
    @Mock MailProvider mailProvider;

    private final Instant now = Instant.parse("2026-11-12T10:15:00Z");
    private InMemoryOrders orders;

    @BeforeEach
    void setUp() {
        orders = new InMemoryOrders();
    }

    @Test
    void handle_createsTheMissingOrderOfAPaidSessionAndLeavesTheCartAlone() {
        paid("cs_1", "cart-1");

        var result = settle();

        assertEquals(1, result.settled());
        assertEquals(0, result.failed());
        var order = orders.all().getFirst();
        assertEquals("cs_1", order.getStripeSessionId().getStripe());
        assertEquals(OrderState.PAID, order.getState());
        assertEquals(now, order.getPaidAt());
        assertEquals(new BigDecimal("66.00"), order.getItemsAmount());
        verify(cartProvider, never()).write(any());
    }

    @Test
    void handle_asksStripeForPaidSessionsOfTheLastThreeDays() {
        settle();

        var since = ArgumentCaptor.forClass(Instant.class);
        verify(checkoutProvider).readIds(eq(null), eq(CheckoutStatus.COMPLETED), since.capture());
        assertEquals(now.minus(SettleOrders.LOOKBACK), since.getValue());
    }

    @Test
    void handle_isIdempotentAndKeepsTheOrderTheCustomerAlreadyCreated() {
        paid("cs_1", "cart-1");
        paid("cs_2", "cart-2");
        // cs_1 a déjà sa commande, créée au retour de la cliente.
        var existing = new me.noynto.eosa.order.Order();
        var sessionId = new CheckoutSessionId();
        sessionId.setStripe("cs_1");
        existing.setStripeSessionId(sessionId);
        existing.setPaidAt(now.minusSeconds(600));
        existing.setState(OrderState.SHIPPED);
        orders.write(existing);

        var first = settle();
        var second = settle();

        assertEquals(1, first.settled());
        assertEquals(0, second.settled());
        assertEquals(2, orders.all().size());
        assertEquals(OrderState.SHIPPED, orders.all().stream()
                .filter(order -> order.getStripeSessionId().getStripe().equals("cs_1")).findFirst().orElseThrow().getState());
    }

    @Test
    void handle_ignoresASessionNotCreatedFromACart() {
        paid("cs_1", null);

        var result = settle();

        assertEquals(0, result.settled());
        assertTrue(orders.all().isEmpty());
    }

    @Test
    void handle_countsAFailureAndStillSettlesTheOthers() {
        var broken = new CheckoutId();
        broken.setStripe("cs_broken");
        paid("cs_ok", "cart-ok");
        when(checkoutProvider.readIds(any(), any(), any())).thenAnswer(inv -> Stream.of(broken, id("cs_ok")));
        when(checkoutProvider.read(broken)).thenReturn(Optional.empty());

        var result = settle();

        assertEquals(1, result.settled());
        assertEquals(1, result.failed());
        assertEquals(1, orders.all().size());
    }

    @Test
    void handle_sendsBothEmailsOfASettledOrderRightAway() {
        paid("cs_1", "cart-1");

        var result = settle();

        assertEquals(2, result.emailsSent());
        var mails = ArgumentCaptor.forClass(Mail.class);
        verify(mailProvider, times(2)).send(mails.capture());
        assertEquals(List.of("contact@eosa.me"), mails.getAllValues().stream()
                .map(Mail::to).filter("contact@eosa.me"::equals).toList());
        var order = orders.all().getFirst();
        assertEquals(now, order.getConfirmationSentAt());
        assertEquals(now, order.getNotificationSentAt());
    }

    @Test
    void handle_resendsTheEmailsThatNeverLeftOnceTheGracePeriodIsOver() {
        var unsent = unsentOrder("cs_old", now.minus(Duration.ofMinutes(6)));
        var tooRecent = unsentOrder("cs_recent", now.minus(Duration.ofMinutes(2)));
        var tooOld = unsentOrder("cs_ancient", now.minus(Duration.ofDays(8)));

        var result = settle();

        assertEquals(0, result.settled());
        assertEquals(2, result.emailsSent());
        assertEquals(now, unsent.getConfirmationSentAt());
        assertEquals(now, unsent.getNotificationSentAt());
        assertNull(tooRecent.getConfirmationSentAt());
        assertNull(tooOld.getConfirmationSentAt());
    }

    @Test
    void handle_neverSendsTwiceAnEmailThatAlreadyLeft() {
        var order = unsentOrder("cs_old", now.minus(Duration.ofHours(1)));
        order.setConfirmationSentAt(now.minus(Duration.ofMinutes(50)));

        settle();
        settle();

        var mails = ArgumentCaptor.forClass(Mail.class);
        verify(mailProvider, times(1)).send(mails.capture());
        assertEquals("contact@eosa.me", mails.getValue().to());
    }

    @Test
    void handle_countsAnEmailFailureAndKeepsItForTheNextPass() {
        var order = unsentOrder("cs_old", now.minus(Duration.ofHours(1)));
        doThrow(new RuntimeException("SMTP indisponible")).when(mailProvider).send(any());

        var result = settle();

        assertEquals(0, result.emailsSent());
        assertEquals(2, result.failed());
        assertNull(order.getConfirmationSentAt());
        assertNull(order.getNotificationSentAt());
    }

    @Test
    void handle_resendsTheShippingEmailThatNeverLeft() {
        var order = shippedOrder("cs_ship", now.minus(Duration.ofHours(1)));

        var result = settle();

        assertEquals(1, result.emailsSent());
        assertEquals(now, order.getShippingSentAt());
        var mails = ArgumentCaptor.forClass(Mail.class);
        verify(mailProvider).send(mails.capture());
        assertEquals("ada@example.com", mails.getValue().to());
        assertEquals("order-shipped", mails.getValue().template());
    }

    @Test
    void handle_waitsForTheGracePeriodAndNeverSendsTheShippingEmailTwice() {
        var tooRecent = shippedOrder("cs_recent", now.minus(Duration.ofMinutes(2)));
        var alreadySent = shippedOrder("cs_sent", now.minus(Duration.ofHours(1)));
        alreadySent.setShippingSentAt(now.minus(Duration.ofMinutes(50)));

        settle();
        settle();

        assertNull(tooRecent.getShippingSentAt());
        verify(mailProvider, never()).send(any());
    }

    @Test
    void handle_resendsTheShippingEmailOfAnOrderOlderThanItsCreationWindow() {
        // Créée il y a 20 jours (hors des 7 jours des e-mails de commande), expédiée il y a une heure.
        var order = shippedOrder("cs_old", now.minus(Duration.ofDays(20)));
        order.setShippedAt(now.minus(Duration.ofHours(1)));

        var result = settle();

        assertEquals(1, result.emailsSent());
        assertEquals(now, order.getShippingSentAt());
    }

    @Test
    void handle_sendsNoEmailForACancelledOrder() {
        var unsent = unsentOrder("cs_cancelled", now.minus(Duration.ofHours(1)));
        unsent.setState(OrderState.CANCELLED);
        var shipped = shippedOrder("cs_cancelled_shipped", now.minus(Duration.ofHours(1)));
        shipped.setState(OrderState.CANCELLED);

        var result = settle();

        assertEquals(0, result.emailsSent());
        assertEquals(0, result.failed());
        assertNull(unsent.getConfirmationSentAt());
        assertNull(shipped.getShippingSentAt());
        verify(mailProvider, never()).send(any());
    }

    @Test
    void handle_countsAShippingEmailFailureAndKeepsItForTheNextPass() {
        var order = shippedOrder("cs_ship", now.minus(Duration.ofHours(1)));
        doThrow(new RuntimeException("SMTP indisponible")).when(mailProvider).send(any());

        var result = settle();

        assertEquals(0, result.emailsSent());
        assertEquals(1, result.failed());
        assertNull(order.getShippingSentAt());
    }

    private Order shippedOrder(String stripeSession, Instant createdAt) {
        var order = unsentOrder(stripeSession, createdAt);
        order.setConfirmationSentAt(createdAt);
        order.setNotificationSentAt(createdAt);
        order.setState(OrderState.SHIPPED);
        order.setShippedAt(createdAt);
        var label = new me.noynto.eosa.shipping.ShippingLabel();
        label.setCarrier(me.noynto.eosa.shipping.ShippingCarrier.MONDIAL_RELAY);
        label.setTrackingNumber("00581108");
        label.setStatus(me.noynto.eosa.shipping.ShippingLabelStatus.CREATED);
        order.setLabel(label);
        return order;
    }

    private Order unsentOrder(String stripeSession, Instant createdAt) {
        var sessionId = new CheckoutSessionId();
        sessionId.setStripe(stripeSession);
        var order = new Order();
        order.setStripeSessionId(sessionId);
        order.setNumber("EOSA-2026-0001");
        order.setState(OrderState.PAID);
        order.setCustomerEmail("ada@example.com");
        order.setItems(List.of());
        order.setItemsAmount(BigDecimal.ZERO);
        order.setShippingAmount(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.ZERO);
        order.setPaidAt(createdAt);
        // La persistance en mémoire date la création du paiement.
        return orders.write(order);
    }

    private SettleOrders.Result settle() {
        return new SettleOrders(checkoutProvider, orders, mailProvider, Clock.fixed(now, ZoneOffset.UTC),
                URI.create("https://admin.eosa.me")).handle();
    }

    private final java.util.List<CheckoutId> paidIds = new java.util.ArrayList<>();

    private CheckoutId id(String stripe) {
        return paidIds.stream().filter(candidate -> stripe.equals(candidate.getStripe())).findFirst().orElseThrow();
    }

    private void paid(String stripeSession, String cartId) {
        var checkoutId = new CheckoutId();
        checkoutId.setStripe(stripeSession);
        var sessionId = new CheckoutSessionId();
        sessionId.setStripe(stripeSession);
        var session = new CheckoutSession();
        session.setId(sessionId);

        var item = new CheckoutItem();
        item.setName("Collier Lune");
        item.setUnitPrice(new BigDecimal("33.00"));
        item.setQuantity(2);

        var checkout = new Checkout();
        checkout.setId(checkoutId);
        checkout.setSession(session);
        checkout.setStatus(CheckoutStatus.COMPLETED);
        checkout.setCartId(cartId != null ? new CartId(cartId) : null);
        checkout.setItems(List.of(item));
        checkout.setShippingAmount(new BigDecimal("4.90"));
        checkout.setTotalAmount(new BigDecimal("70.90"));

        paidIds.add(checkoutId);
        when(checkoutProvider.readIds(any(), any(), any())).thenAnswer(inv -> paidIds.stream());
        when(checkoutProvider.read(checkoutId)).thenReturn(Optional.of(checkout));
    }

}
