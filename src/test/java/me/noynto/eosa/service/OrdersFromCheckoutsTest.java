package me.noynto.eosa.service;

import me.noynto.eosa.checkout.Checkout;
import me.noynto.eosa.checkout.CheckoutItem;
import me.noynto.eosa.checkout.CheckoutSession;
import me.noynto.eosa.order.OrderState;
import me.noynto.eosa.shared.CheckoutSessionId;
import me.noynto.eosa.shared.JewelId;
import me.noynto.eosa.shared.PromoCodeId;
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
    void keepsThePromoCodeAndItsDiscount() {
        var checkout = checkout();
        checkout.setItems(List.of(item("Collier Lune", "33.00", 2)));
        checkout.setShippingAmount(new BigDecimal("4.90"));
        checkout.setDiscountAmount(new BigDecimal("6.60"));
        checkout.setPromoCodeId(new PromoCodeId("p-1"));
        checkout.setPromoCode("BIENVENUE10");

        var order = OrdersFromCheckouts.fromPaidCheckout(checkout, paidAt);

        assertEquals(new BigDecimal("66.00"), order.getItemsAmount());
        assertEquals(new BigDecimal("6.60"), order.getDiscountAmount());
        assertEquals("BIENVENUE10", order.getPromoCode());
        assertEquals("p-1", order.getPromoCodeId().value());
        // Sans total de Stripe : bijoux, moins la remise, plus la livraison.
        assertEquals(new BigDecimal("64.30"), order.getTotalAmount());
    }

    @Test
    void keepsTheGiftCardAndItsPartWithoutAnyPromoDiscount() {
        var checkout = checkout();
        checkout.setItems(List.of(item("Collier Lune", "33.00", 2)));
        checkout.setShippingAmount(new BigDecimal("4.90"));
        checkout.setUsedGiftCardId(new me.noynto.eosa.shared.GiftCardId("card-1"));
        checkout.setUsedGiftCardAmount(new BigDecimal("20.00"));

        var order = OrdersFromCheckouts.fromPaidCheckout(checkout, paidAt);

        assertEquals("card-1", order.getGiftCardId().value());
        assertEquals(new BigDecimal("20.00"), order.getGiftCardAmount());
        assertEquals(0, BigDecimal.ZERO.compareTo(order.getDiscountAmount()));
        // Sans total de Stripe : bijoux, moins la part de la carte, plus la livraison.
        assertEquals(new BigDecimal("50.90"), order.getTotalAmount());
    }

    @Test
    void ignoresThePromoCodeWhenItDidNotDiscountAnything() {
        var checkout = checkout();
        checkout.setItems(List.of(item("Collier Lune", "33.00", 1)));
        checkout.setDiscountAmount(BigDecimal.ZERO);
        checkout.setPromoCodeId(new PromoCodeId("p-1"));
        checkout.setPromoCode("VIDE");

        var order = OrdersFromCheckouts.fromPaidCheckout(checkout, paidAt);

        assertEquals(0, order.getDiscountAmount().signum());
        assertNull(order.getPromoCode());
        assertNull(order.getPromoCodeId());
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
