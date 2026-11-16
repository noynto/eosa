package me.noynto.eosa.application;

import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.shared.CheckoutSessionId;
import me.noynto.eosa.shared.OrderId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeliverAndCancelOrderTest {

    private final Instant now = Instant.parse("2026-11-12T10:15:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private InMemoryOrders orders;
    private Order order;

    @BeforeEach
    void setUp() {
        orders = new InMemoryOrders();
        var sessionId = new CheckoutSessionId();
        sessionId.setStripe("cs_1");
        order = new Order();
        order.setStripeSessionId(sessionId);
        order.setState(OrderState.SHIPPED);
        order.setItems(List.of());
        order.setItemsAmount(BigDecimal.ZERO);
        order.setShippingAmount(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.ZERO);
        order.setPaidAt(now.minusSeconds(86_400));
        orders.write(order);
    }

    @Test
    void deliver_marksAShippedOrderDelivered() {
        deliver();

        assertEquals(OrderState.DELIVERED, order.getState());
        assertEquals(now, order.getDeliveredAt());
    }

    @Test
    void deliver_refusesAnOrderThatIsNotShipped() {
        for (var state : List.of(OrderState.PAID, OrderState.CRAFTED, OrderState.DELIVERED, OrderState.CANCELLED)) {
            order.setState(state);
            assertThrows(RuntimeException.class, this::deliver, state.name());
        }

        assertNull(order.getDeliveredAt());
    }

    @Test
    void deliver_refusesAnUnknownOrder() {
        assertThrows(RuntimeException.class, () -> new DeliverOrder(orders, clock)
                .handle(new DeliverOrder.Command(new OrderId("unknown"))));
    }

    @Test
    void cancel_cancelsAnOrderToCraftCraftedOrShippedAndKeepsTheReason() {
        for (var state : List.of(OrderState.PAID, OrderState.CRAFTED, OrderState.SHIPPED)) {
            order.setState(state);
            order.setCancelledAt(null);
            order.setCancelReason(null);

            cancel("  Cliente injoignable  ");

            assertEquals(OrderState.CANCELLED, order.getState(), state.name());
            assertEquals(now, order.getCancelledAt());
            assertEquals("Cliente injoignable", order.getCancelReason());
        }
    }

    @Test
    void cancel_refusesADeliveredOrAlreadyCancelledOrder() {
        for (var state : List.of(OrderState.DELIVERED, OrderState.CANCELLED)) {
            order.setState(state);
            assertThrows(RuntimeException.class, () -> cancel("motif"), state.name());
        }

        assertNull(order.getCancelledAt());
    }

    @Test
    void cancel_needsAReasonOfAReasonableLength() {
        assertThrows(RuntimeException.class, () -> cancel(null));
        assertThrows(RuntimeException.class, () -> cancel("   "));
        assertThrows(RuntimeException.class, () -> cancel("x".repeat(CancelOrder.MAX_REASON_LENGTH + 1)));

        assertEquals(OrderState.SHIPPED, order.getState());
    }

    @Test
    void cancel_refusesAnUnknownOrder() {
        assertThrows(RuntimeException.class, () -> new CancelOrder(orders, clock)
                .handle(new CancelOrder.Command(new OrderId("unknown"), "motif")));
    }

    private void deliver() {
        new DeliverOrder(orders, clock).handle(new DeliverOrder.Command(order.getId()));
    }

    private void cancel(String reason) {
        new CancelOrder(orders, clock).handle(new CancelOrder.Command(order.getId(), reason));
    }

}
