package me.noynto.eosa.application;

import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.shared.CheckoutSessionId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReadOrdersTest {

    // Mercredi 18 novembre 2026, 12 h à Paris.
    private final Instant now = LocalDateTime.of(2026, 11, 18, 12, 0)
            .atZone(me.noynto.eosa.service.BusinessTime.ZONE).toInstant();
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private InMemoryOrders orders;

    @BeforeEach
    void setUp() {
        orders = new InMemoryOrders();
    }

    @Test
    void handle_filtersByStateAndListsTheMostRecentFirst() {
        var older = order("cs_1", "Ada", "ada@example.com", now.minusSeconds(7200), OrderState.PAID);
        var newer = order("cs_2", "Grace", "grace@example.com", now.minusSeconds(3600), OrderState.PAID);
        order("cs_3", "Alan", "alan@example.com", now.minusSeconds(1800), OrderState.SHIPPED);

        var result = new ReadOrders(orders, clock).handle(new ReadOrders.Command(EnumSet.of(OrderState.PAID), null));

        assertEquals(List.of(newer.getId(), older.getId()),
                result.orders().stream().map(entry -> entry.order().getId()).toList());
    }

    @Test
    void handle_searchesByNumberNameOrEmailWithoutCaseNorAccentsOfCase() {
        var ada = order("cs_1", "Ada Lovelace", "ada@example.com", now.minusSeconds(60), OrderState.PAID);
        order("cs_2", "Grace Hopper", "grace@example.com", now.minusSeconds(30), OrderState.PAID);

        var reader = new ReadOrders(orders, clock);

        assertEquals(1, reader.handle(new ReadOrders.Command(null, "LOVELACE")).orders().size());
        assertEquals(1, reader.handle(new ReadOrders.Command(null, "grace@")).orders().size());
        assertEquals(1, reader.handle(new ReadOrders.Command(null, ada.getNumber())).orders().size());
        assertEquals(2, reader.handle(new ReadOrders.Command(null, "  ")).orders().size());
    }

    @Test
    void handle_flagsAnOrderToCraftPastItsFortyEightBusinessHours() {
        // Payée le lundi 16 novembre à 10 h : échéance mercredi 18 à 10 h, dépassée à 12 h.
        var late = order("cs_1", "Ada", "ada@example.com",
                LocalDateTime.of(2026, 11, 16, 10, 0).atZone(me.noynto.eosa.service.BusinessTime.ZONE).toInstant(),
                OrderState.PAID);
        // Payée le mardi 17 à 20 h : encore dans les délais.
        var onTime = order("cs_2", "Grace", "grace@example.com",
                LocalDateTime.of(2026, 11, 17, 20, 0).atZone(me.noynto.eosa.service.BusinessTime.ZONE).toInstant(),
                OrderState.PAID);
        // Une commande déjà fabriquée n'est jamais en retard de fabrication.
        var crafted = order("cs_3", "Alan", "alan@example.com",
                LocalDateTime.of(2026, 11, 2, 10, 0).atZone(me.noynto.eosa.service.BusinessTime.ZONE).toInstant(),
                OrderState.CRAFTED);

        var entries = new ReadOrders(orders, clock).handle(new ReadOrders.Command(null, null)).orders();

        assertTrue(overdue(entries, late));
        assertFalse(overdue(entries, onTime));
        assertFalse(overdue(entries, crafted));
    }

    @Test
    void readOrder_givesTheOrderWithItsDeadline() {
        var paidAt = LocalDateTime.of(2026, 11, 16, 10, 0).atZone(me.noynto.eosa.service.BusinessTime.ZONE).toInstant();
        var order = order("cs_1", "Ada", "ada@example.com", paidAt, OrderState.PAID);

        var result = new ReadOrder(orders, clock, 150).handle(new ReadOrder.Command(order.getId()));

        assertEquals(150, result.defaultParcelWeightGrams());
        assertEquals(order.getId(), result.order().getId());
        assertEquals(LocalDateTime.of(2026, 11, 18, 10, 0).atZone(me.noynto.eosa.service.BusinessTime.ZONE).toInstant(),
                result.craftingDeadline());
        assertTrue(result.overdue());
    }

    @Test
    void readOrder_refusesAnUnknownOrder() {
        assertThrows(RuntimeException.class, () -> new ReadOrder(orders, clock, 150)
                .handle(new ReadOrder.Command(new me.noynto.eosa.shared.OrderId("unknown"))));
    }

    @Test
    void handle_flagsACraftedOrderNotShippedWithinTwentyFourBusinessHours() {
        // Fabriquée le lundi 16 novembre à 10 h : à expédier avant le mardi 17 à 10 h, dépassé le mercredi 18 à 12 h.
        var late = order("cs_1", "Ada", "ada@example.com",
                LocalDateTime.of(2026, 11, 2, 10, 0).atZone(me.noynto.eosa.service.BusinessTime.ZONE).toInstant(),
                OrderState.CRAFTED);
        late.setCraftedAt(LocalDateTime.of(2026, 11, 16, 10, 0).atZone(me.noynto.eosa.service.BusinessTime.ZONE).toInstant());
        // Fabriquée le mardi 17 à 14 h : à expédier avant le mercredi 18 à 14 h, encore dans les délais.
        var onTime = order("cs_2", "Grace", "grace@example.com",
                LocalDateTime.of(2026, 11, 2, 10, 0).atZone(me.noynto.eosa.service.BusinessTime.ZONE).toInstant(),
                OrderState.CRAFTED);
        onTime.setCraftedAt(LocalDateTime.of(2026, 11, 17, 14, 0).atZone(me.noynto.eosa.service.BusinessTime.ZONE).toInstant());

        var entries = new ReadOrders(orders, clock).handle(new ReadOrders.Command(null, null)).orders();

        assertTrue(overdue(entries, late));
        assertFalse(overdue(entries, onTime));
        assertTrue(new ReadOrder(orders, clock, 150).handle(new ReadOrder.Command(late.getId())).overdue());
    }

    private boolean overdue(List<ReadOrders.Entry> entries, Order order) {
        return entries.stream().filter(entry -> entry.order().getId().equals(order.getId())).findFirst().orElseThrow().overdue();
    }

    private Order order(String stripeSession, String name, String email, Instant paidAt, OrderState state) {
        var sessionId = new CheckoutSessionId();
        sessionId.setStripe(stripeSession);
        var order = new Order();
        order.setStripeSessionId(sessionId);
        order.setCustomerName(name);
        order.setCustomerEmail(email);
        order.setPaidAt(paidAt);
        order.setState(state);
        order.setItems(List.of());
        order.setItemsAmount(BigDecimal.ZERO);
        order.setShippingAmount(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.ZERO);
        return orders.write(order);
    }

}
