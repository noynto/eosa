package me.noynto.eosa.application;

import me.noynto.eosa.order.Order;
import me.noynto.eosa.order.OrderItem;
import me.noynto.eosa.order.OrderShippingAddress;
import me.noynto.eosa.order.OrderState;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PurgeOrderPersonalDataTest {

    private final Instant now = Instant.parse("2027-11-12T10:15:00Z");
    private InMemoryOrders orders;
    private int sessions;

    @BeforeEach
    void setUp() {
        orders = new InMemoryOrders();
    }

    @Test
    void handle_erasesTheAddressAndThePhoneOfAnOrderDeliveredMoreThanAYearAgo() {
        var order = order(OrderState.DELIVERED);
        order.setDeliveredAt(Instant.parse("2026-11-10T10:00:00Z"));

        var result = purge();

        assertEquals(1, result.purged());
        assertNull(order.getShippingAddress());
        assertNull(order.getCustomerPhone());
        assertEquals(now, order.getPersonalDataPurgedAt());
    }

    @Test
    void handle_keepsTheAccountingPartsOfThePurgedOrder() {
        var order = order(OrderState.DELIVERED);
        order.setDeliveredAt(Instant.parse("2026-01-10T10:00:00Z"));

        purge();

        assertEquals(new BigDecimal("70.90"), order.getTotalAmount());
        assertEquals(new BigDecimal("66.00"), order.getItemsAmount());
        assertEquals(1, order.getItems().size());
        assertEquals("EOSA-2026-0001", order.getNumber());
        assertEquals("ada@example.com", order.getCustomerEmail());
        assertEquals("Ada Lovelace", order.getCustomerName());
    }

    @Test
    void handle_countsTheYearFromTheCancellationOfACancelledOrder() {
        var old = order(OrderState.CANCELLED);
        old.setCancelledAt(Instant.parse("2026-10-01T10:00:00Z"));
        var recent = order(OrderState.CANCELLED);
        recent.setCancelledAt(Instant.parse("2027-10-01T10:00:00Z"));

        var result = purge();

        assertEquals(1, result.purged());
        assertNotNull(old.getPersonalDataPurgedAt());
        assertNull(recent.getPersonalDataPurgedAt());
        assertNotNull(recent.getShippingAddress());
    }

    @Test
    void handle_waitsTheFullYearInTheParisTimeZone() {
        // Livrée le 12 novembre 2026 à 11 h 30 à Paris (10 h 30 UTC) : le 12 novembre 2027 à 11 h 15, il manque 15 minutes.
        var order = order(OrderState.DELIVERED);
        order.setDeliveredAt(LocalDateTime.of(2026, 11, 12, 11, 30).atZone(me.noynto.eosa.service.BusinessTime.ZONE).toInstant());

        var tooEarly = purgeAt(LocalDateTime.of(2027, 11, 12, 11, 15).atZone(me.noynto.eosa.service.BusinessTime.ZONE).toInstant());
        var onTime = purgeAt(LocalDateTime.of(2027, 11, 12, 11, 30).atZone(me.noynto.eosa.service.BusinessTime.ZONE).toInstant());

        assertEquals(0, tooEarly.purged());
        assertEquals(1, onTime.purged());
    }

    @Test
    void handle_neverTouchesAnOrderThatIsNeitherDeliveredNorCancelled() {
        for (var state : List.of(OrderState.PAID, OrderState.CRAFTED, OrderState.SHIPPED)) {
            var order = order(state);
            // Très ancienne : la cliente attend peut-être encore son colis.
            order.setPaidAt(Instant.parse("2020-01-01T10:00:00Z"));
        }

        var result = purge();

        assertEquals(0, result.purged());
        assertEquals(0, result.failed());
        orders.all().forEach(order -> assertNotNull(order.getShippingAddress()));
    }

    @Test
    void handle_purgesOnlyOnce() {
        var order = order(OrderState.DELIVERED);
        order.setDeliveredAt(Instant.parse("2026-01-10T10:00:00Z"));

        var first = purge();
        var later = new PurgeOrderPersonalData(orders, Clock.fixed(now.plusSeconds(86_400), ZoneOffset.UTC)).handle();

        assertEquals(1, first.purged());
        assertEquals(0, later.purged());
        assertEquals(now, order.getPersonalDataPurgedAt());
    }

    @Test
    void handle_fallsBackOnTheLastKnownStepWhenTheDeliveryDateIsMissing() {
        // Commande livrée avant que la date de livraison soit tracée : on part de l'expédition, jamais trop tôt.
        var order = order(OrderState.DELIVERED);
        order.setShippedAt(Instant.parse("2027-10-01T10:00:00Z"));
        var older = order(OrderState.DELIVERED);
        older.setShippedAt(Instant.parse("2026-01-01T10:00:00Z"));

        var result = purge();

        assertEquals(1, result.purged());
        assertNotNull(order.getShippingAddress());
        assertNull(older.getShippingAddress());
    }

    private PurgeOrderPersonalData.Result purge() {
        return purgeAt(now);
    }

    private PurgeOrderPersonalData.Result purgeAt(Instant at) {
        return new PurgeOrderPersonalData(orders, Clock.fixed(at, ZoneOffset.UTC)).handle();
    }

    private Order order(OrderState state) {
        var sessionId = new CheckoutSessionId();
        sessionId.setStripe("cs_" + (++sessions));
        var item = new OrderItem();
        item.setName("Collier Lune");
        item.setUnitPrice(new BigDecimal("33.00"));
        item.setQuantity(2);
        var order = new Order();
        order.setStripeSessionId(sessionId);
        order.setState(state);
        order.setCustomerName("Ada Lovelace");
        order.setCustomerEmail("ada@example.com");
        order.setCustomerPhone("+33600000000");
        order.setShippingAddress(new OrderShippingAddress("Ada Lovelace", "1 rue des Arts", null, "54000", "Nancy", "FR"));
        order.setItems(List.of(item));
        order.setItemsAmount(new BigDecimal("66.00"));
        order.setShippingAmount(new BigDecimal("4.90"));
        order.setTotalAmount(new BigDecimal("70.90"));
        order.setPaidAt(Instant.parse("2026-10-01T10:00:00Z").plusSeconds(sessions));
        return orders.write(order);
    }

}
