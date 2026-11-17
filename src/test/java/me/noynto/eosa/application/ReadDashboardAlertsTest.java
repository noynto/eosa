package me.noynto.eosa.application;

import me.noynto.eosa.giftcard.GiftCard;
import me.noynto.eosa.giftcard.GiftCardMovement;
import me.noynto.eosa.giftcard.GiftCardMovementKind;
import me.noynto.eosa.giftcard.GiftCardProvider;
import me.noynto.eosa.giftcard.GiftCardState;
import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.shared.CheckoutSessionId;
import me.noynto.eosa.shared.GiftCardId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReadDashboardAlertsTest {

    @Mock GiftCardProvider giftCardProvider;

    private final Instant now = Instant.parse("2026-11-18T11:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private InMemoryOrders orders;

    @BeforeEach
    void setUp() {
        orders = new InMemoryOrders();
        when(giftCardProvider.readIds(any(), any())).thenAnswer(inv -> Stream.empty());
    }

    private Order order(String session, Instant paidAt, OrderState state, Instant confirmationSentAt, Instant notificationSentAt) {
        var sessionId = new CheckoutSessionId();
        sessionId.setStripe(session);
        var order = new Order();
        order.setStripeSessionId(sessionId);
        order.setPaidAt(paidAt);
        order.setState(state);
        order.setItems(List.of());
        order.setItemsAmount(BigDecimal.ZERO);
        order.setShippingAmount(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.ZERO);
        order.setConfirmationSentAt(confirmationSentAt);
        order.setNotificationSentAt(notificationSentAt);
        return orders.write(order);
    }

    private GiftCard card(String id, GiftCardState state, Instant emailSentAt, Instant issuedAt) {
        var giftCard = new GiftCard();
        giftCard.setId(new GiftCardId(id));
        giftCard.setState(state);
        giftCard.setBuyerEmail("lea@example.com");
        giftCard.setEmailSentAt(emailSentAt);
        giftCard.setCreatedAt(issuedAt);
        giftCard.getMovements().add(new GiftCardMovement(GiftCardMovementKind.ISSUED, new BigDecimal("50.00"), null, issuedAt));
        when(giftCardProvider.read(giftCard.getId())).thenReturn(Optional.of(giftCard));
        return giftCard;
    }

    @Test
    void handle_listsTheOrdersWhoseEmailDidNotLeaveOnceTheGracePeriodIsOver() {
        var tenMinutes = now.minus(Duration.ofMinutes(10));
        order("cs_1", tenMinutes, OrderState.PAID, now, null);
        order("cs_2", tenMinutes, OrderState.PAID, null, now);
        order("cs_3", tenMinutes, OrderState.PAID, now, now);
        // Encore en train de partir.
        order("cs_4", now.minus(Duration.ofMinutes(2)), OrderState.PAID, null, null);
        // Annulée : plus d'e-mail.
        order("cs_5", tenMinutes, OrderState.CANCELLED, null, null);
        // Trop ancienne : plus renvoyée.
        order("cs_6", now.minus(Duration.ofDays(8)), OrderState.PAID, null, null);

        var alerts = new ReadDashboardAlerts(orders, giftCardProvider, clock).handle();

        assertEquals(2, alerts.orders().size());
        assertTrue(alerts.giftCards().isEmpty());
    }

    @Test
    void handle_listsTheActiveCardsWhoseEmailDidNotLeave() {
        var tenMinutes = now.minus(Duration.ofMinutes(10));
        var late = card("late", GiftCardState.ACTIVE, null, tenMinutes);
        var sent = card("sent", GiftCardState.ACTIVE, now, tenMinutes);
        var pending = card("pending", GiftCardState.PENDING_PAYMENT, null, tenMinutes);
        var young = card("young", GiftCardState.ACTIVE, null, now.minus(Duration.ofMinutes(1)));
        var old = card("old", GiftCardState.ACTIVE, null, now.minus(Duration.ofDays(9)));
        when(giftCardProvider.readIds(any(), any())).thenAnswer(inv ->
                Stream.of(late, sent, pending, young, old).map(GiftCard::getId));

        var alerts = new ReadDashboardAlerts(orders, giftCardProvider, clock).handle();

        assertEquals(List.of(new GiftCardId("late")), alerts.giftCards());
        assertTrue(alerts.orders().isEmpty());
    }

    @Test
    void handle_isEmptyWhenAllEmailsLeft() {
        order("cs_1", now.minus(Duration.ofHours(1)), OrderState.PAID, now, now);

        assertTrue(new ReadDashboardAlerts(orders, giftCardProvider, clock).handle().isEmpty());
    }

}
