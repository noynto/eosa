package me.noynto.eosa.service;

import me.noynto.eosa.checkout.Checkout;
import me.noynto.eosa.checkout.CheckoutItem;
import me.noynto.eosa.checkout.CheckoutSession;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.shared.CheckoutSessionId;
import me.noynto.eosa.shared.JewelId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrdersFromCheckoutsTest {

    private final Instant paidAt = Instant.parse("2026-11-12T10:15:00Z");

    @Test
    void copiesLinesAndKeepsTheTotalPaidAtStripe() {
        var checkout = checkout();
        checkout.setItems(List.of(item("Collier Lune", "33.00", 2), item("Bracelet Soleil", "23.00", 1)));
        checkout.setShippingAmount(new BigDecimal("4.90"));
        checkout.setTotalAmount(new BigDecimal("90.00"));

        var order = OrdersFromCheckouts.fromPaidCheckout(checkout, paidAt);

        assertEquals(OrderState.PAID, order.getState());
        assertEquals(2, order.getItems().size());
        assertEquals(new BigDecimal("89.00"), order.getItemsAmount());
        assertEquals(new BigDecimal("4.90"), order.getShippingAmount());
        // Le total est celui de Stripe, même s'il diffère d'un recalcul.
        assertEquals(new BigDecimal("90.00"), order.getTotalAmount());
        assertEquals(paidAt, order.getPaidAt());
        assertEquals("cs_test_1", order.getStripeSessionId().getStripe());
        assertNull(order.getId());
        assertNull(order.getNumber());
    }

    @Test
    void recomputesTheTotalWhenStripeDidNotGiveIt() {
        var checkout = checkout();
        checkout.setItems(List.of(item("Collier Lune", "33.00", 1)));
        checkout.setShippingAmount(new BigDecimal("4.90"));

        var order = OrdersFromCheckouts.fromPaidCheckout(checkout, paidAt);

        assertEquals(new BigDecimal("37.90"), order.getTotalAmount());
    }

    @Test
    void treatsMissingShippingAndLinesAsZeroAndEmpty() {
        var order = OrdersFromCheckouts.fromPaidCheckout(checkout(), paidAt);

        assertEquals(List.of(), order.getItems());
        assertEquals(new BigDecimal("0"), order.getItemsAmount());
        assertEquals(BigDecimal.ZERO, order.getShippingAmount());
        assertNull(order.getShippingAddress());
    }

    @Test
    void keepsTheJewelOfALineWhenKnown() {
        var checkout = checkout();
        var line = item("Collier Lune", "33.00", 1);
        line.setJewelId(new JewelId("jewel"));
        checkout.setItems(List.of(line));

        var order = OrdersFromCheckouts.fromPaidCheckout(checkout, paidAt);

        assertEquals(new JewelId("jewel"), order.getItems().getFirst().getJewelId());
    }

    @Test
    void refusesACheckoutWithoutSession() {
        assertThrows(RuntimeException.class, () -> OrdersFromCheckouts.fromPaidCheckout(new Checkout(), paidAt));
    }

    private Checkout checkout() {
        var sessionId = new CheckoutSessionId();
        sessionId.setStripe("cs_test_1");
        var session = new CheckoutSession();
        session.setId(sessionId);
        var checkout = new Checkout();
        checkout.setSession(session);
        return checkout;
    }

    private CheckoutItem item(String name, String unitPrice, int quantity) {
        var item = new CheckoutItem();
        item.setName(name);
        item.setUnitPrice(new BigDecimal(unitPrice));
        item.setQuantity(quantity);
        return item;
    }

}
