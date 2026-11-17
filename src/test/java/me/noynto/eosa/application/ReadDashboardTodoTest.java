package me.noynto.eosa.application;

import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.service.BusinessTime;
import me.noynto.eosa.shared.CheckoutSessionId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReadDashboardTodoTest {

    // Mercredi 18 novembre 2026, 12 h à Paris.
    private final Instant now = at(2026, 11, 18, 12);
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private InMemoryOrders orders;

    @BeforeEach
    void setUp() {
        orders = new InMemoryOrders();
    }

    private static Instant at(int year, int month, int day, int hour) {
        return LocalDateTime.of(year, month, day, hour, 0).atZone(BusinessTime.ZONE).toInstant();
    }

    private Order order(String session, Instant paidAt, OrderState state) {
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
        return orders.write(order);
    }

    @Test
    void handle_countsEachStageAndFindsTheTightestCraftingDeadline() {
        // Payée le lundi 16 à 10 h : échéance mercredi 18 à 10 h, dépassée à 12 h.
        order("cs_1", at(2026, 11, 16, 10), OrderState.PAID);
        // Payée le mardi 17 à 10 h : échéance jeudi 19 à 10 h.
        order("cs_2", at(2026, 11, 17, 10), OrderState.PAID);
        var crafted = order("cs_3", at(2026, 11, 15, 10), OrderState.CRAFTED);
        crafted.setCraftedAt(at(2026, 11, 17, 9));
        order("cs_4", at(2026, 11, 10, 10), OrderState.SHIPPED);
        order("cs_5", at(2026, 11, 10, 10), OrderState.DELIVERED);
        order("cs_6", at(2026, 11, 10, 10), OrderState.CANCELLED);

        var todo = new ReadDashboardTodo(orders, clock).handle();

        assertEquals(2, todo.toCraft());
        assertEquals(1, todo.craftOverdue());
        assertEquals(at(2026, 11, 18, 10), todo.nextCraftDeadline());
        assertEquals(1, todo.toShip());
        assertEquals(1, todo.shipOverdue(), "fabriquée mardi 9 h : 24 h ouvrées écoulées mercredi 9 h");
        assertEquals(1, todo.inTransit());
    }

    @Test
    void handle_neverFlagsAHandDeliveryAsLateToShip() {
        var crafted = order("cs_1", at(2026, 11, 10, 10), OrderState.CRAFTED);
        crafted.setCraftedAt(at(2026, 11, 10, 10));
        crafted.setHandDelivery(true);

        var todo = new ReadDashboardTodo(orders, clock).handle();

        assertEquals(1, todo.toShip());
        assertEquals(0, todo.shipOverdue());
    }

    @Test
    void handle_isEmptyWithoutOrders() {
        var todo = new ReadDashboardTodo(orders, clock).handle();

        assertEquals(0, todo.toCraft());
        assertEquals(0, todo.toShip());
        assertEquals(0, todo.inTransit());
        assertNull(todo.nextCraftDeadline());
    }

}
