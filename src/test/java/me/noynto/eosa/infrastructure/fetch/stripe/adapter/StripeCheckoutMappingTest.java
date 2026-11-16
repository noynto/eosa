package me.noynto.eosa.infrastructure.fetch.stripe.adapter;

import com.google.gson.Gson;
import me.noynto.eosa.checkout.CheckoutStatus;
import me.noynto.eosa.infrastructure.fetch.stripe.resource.StripeCheckoutSessionResource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Lecture d'une session Stripe payée, sur des réponses JSON d'exemple (sans appel réseau).
 */
class StripeCheckoutMappingTest {

    private final StripeFetchedCheckouts adapter = new StripeFetchedCheckouts(null, "https://eosa.me");

    @Test
    void readsCustomerShippingAddressAndAmounts() {
        var checkout = adapter.toCheckout(session("""
                {
                  "id": "cs_test_1",
                  "payment_status": "paid",
                  "client_reference_id": "cart-1",
                  "amount_total": 7090,
                  "shipping_cost": {"amount_total": 490},
                  "customer_details": {"email": "ada@example.com", "name": "Ada", "phone": "+33600000000"},
                  "shipping_details": {
                    "name": "Ada Lovelace",
                    "address": {"line1": "1 rue des Arts", "line2": "Bât. B", "postal_code": "54000",
                                "city": "Nancy", "country": "FR"}
                  }
                }
                """));

        assertEquals(CheckoutStatus.COMPLETED, checkout.getStatus());
        assertEquals("cart-1", checkout.getCartId().value());
        assertEquals(new BigDecimal("70.90"), checkout.getTotalAmount());
        assertEquals(new BigDecimal("4.90"), checkout.getShippingAmount());
        assertEquals("ada@example.com", checkout.getCustomerEmail());
        assertEquals("+33600000000", checkout.getCustomerPhone());
        var address = checkout.getShippingAddress();
        assertEquals("Ada Lovelace", address.name());
        assertEquals("1 rue des Arts", address.line1());
        assertEquals("Bât. B", address.line2());
        assertEquals("54000", address.postalCode());
        assertEquals("Nancy", address.city());
        assertEquals("FR", address.country());
    }

    @Test
    void readsTheShippingAddressOfNewerApiVersions() {
        var checkout = adapter.toCheckout(session("""
                {
                  "id": "cs_test_2",
                  "payment_status": "paid",
                  "collected_information": {
                    "shipping_details": {
                      "name": "Ada Lovelace",
                      "address": {"line1": "1 rue des Arts", "postal_code": "54000", "city": "Nancy", "country": "FR"}
                    }
                  }
                }
                """));

        assertEquals("Nancy", checkout.getShippingAddress().city());
        assertNull(checkout.getShippingAddress().line2());
    }

    @Test
    void readsTheDiscountAndThePromoCodeOfTheSession() {
        var checkout = adapter.toCheckout(session("""
                {
                  "id": "cs_test_4",
                  "payment_status": "paid",
                  "amount_total": 6090,
                  "total_details": {"amount_discount": 1000},
                  "metadata": {"promo_code_id": "p-1", "promo_code": "BIENVENUE10"}
                }
                """));

        assertEquals(new BigDecimal("10.00"), checkout.getDiscountAmount());
        assertEquals("p-1", checkout.getPromoCodeId().value());
        assertEquals("BIENVENUE10", checkout.getPromoCode());
    }

    @Test
    void readsTheGiftCardPartOfTheSessionAsItsDiscountNotAsAPromoDiscount() {
        var checkout = adapter.toCheckout(session("""
                {
                  "id": "cs_test_5",
                  "payment_status": "paid",
                  "client_reference_id": "cart-1",
                  "amount_total": 2290,
                  "total_details": {"amount_discount": 2000},
                  "metadata": {"used_gift_card_id": "card-1", "used_gift_card_amount": "2000"}
                }
                """));

        assertEquals("card-1", checkout.getUsedGiftCardId().value());
        assertEquals(new BigDecimal("20.00"), checkout.getUsedGiftCardAmount());
        assertEquals(0, BigDecimal.ZERO.compareTo(checkout.getDiscountAmount()));
        assertNull(checkout.getPromoCodeId());
        assertNull(checkout.getGiftCardId(), "la carte utilisée n'est pas une carte achetée");
    }

    @Test
    void readsACartCoveredByTheCardAtZeroEuroAsPaid() {
        var checkout = adapter.toCheckout(session("""
                {
                  "id": "cs_test_6",
                  "payment_status": "no_payment_required",
                  "client_reference_id": "cart-1",
                  "amount_total": 0,
                  "total_details": {"amount_discount": 7000},
                  "metadata": {"used_gift_card_id": "card-1"}
                }
                """));

        assertEquals(CheckoutStatus.COMPLETED, checkout.getStatus());
        assertEquals(new BigDecimal("0.00"), checkout.getTotalAmount());
        assertEquals(new BigDecimal("70.00"), checkout.getUsedGiftCardAmount());
    }

    @Test
    void leavesAmountsAndAddressEmptyWhenStripeDidNotGiveThem() {
        var checkout = adapter.toCheckout(session("""
                {"id": "cs_test_3", "payment_status": "unpaid"}
                """));

        assertEquals(CheckoutStatus.PENDING, checkout.getStatus());
        assertNull(checkout.getTotalAmount());
        assertNull(checkout.getShippingAmount());
        assertNull(checkout.getDiscountAmount());
        assertNull(checkout.getPromoCodeId());
        assertNull(checkout.getShippingAddress());
        assertNull(checkout.getCustomerEmail());
    }

    private StripeCheckoutSessionResource.CheckoutSession session(String json) {
        return new Gson().fromJson(json, StripeCheckoutSessionResource.CheckoutSession.class);
    }

}
