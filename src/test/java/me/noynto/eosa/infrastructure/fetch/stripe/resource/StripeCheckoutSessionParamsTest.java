package me.noynto.eosa.infrastructure.fetch.stripe.resource;

import me.noynto.eosa.infrastructure.fetch.stripe.config.StripeHttpClient;
import me.noynto.eosa.infrastructure.fetch.stripe.config.StripeProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Paramètres de la session de paiement, sans appel réseau : la remise d'un code de promotion n'arrive chez Stripe
 * que comme un coupon rattaché à la session, avec le code en métadonnées.
 */
class StripeCheckoutSessionParamsTest {

    private final StripeCheckoutSessionResource resource = new StripeCheckoutSessionResource(
            new StripeHttpClient(null, new StripeProperties("sk_test_x", "https://shop/success", "https://shop/cart")));

    private final List<StripeCheckoutSessionResource.LineItem> items = List.of(
            new StripeCheckoutSessionResource.LineItem("Collier Lune", new BigDecimal("40.00"), 1, null, null, null));

    @Test
    void attachesTheCouponAndTheCodeToTheSession() {
        var discount = new StripeCheckoutSessionResource.Discount(new BigDecimal("4.00"), "p-1", "BIENVENUE10");

        var params = resource.buildParams(items, "cart-1", new BigDecimal("4.90"), discount, "coupon_1");

        assertTrue(params.contains("discounts[0][coupon]=coupon_1"), params.toString());
        assertTrue(params.contains("metadata[promo_code_id]=p-1"), params.toString());
        assertTrue(params.contains("metadata[promo_code]=BIENVENUE10"), params.toString());
        // Le prix de la ligne reste celui du catalogue.
        assertTrue(params.contains("line_items[0][price_data][unit_amount]=4000"), params.toString());
    }

    @Test
    void giftCardSessionHasNoCartNoShippingNoAddressAndCarriesTheCardInMetadata() {
        var params = resource.buildGiftCardParams(
                new StripeCheckoutSessionResource.LineItem("Carte cadeau EOSA", new BigDecimal("50.00"), 1, null, null, null),
                "card-1", "lea@example.com", 1_700_000_000L, "https://shop/gift-cards/success", "https://shop/gift-cards");

        assertTrue(params.contains("metadata[gift_card_id]=card-1"), params.toString());
        assertTrue(params.contains("expires_at=1700000000"), params.toString());
        assertTrue(params.contains("line_items[0][price_data][unit_amount]=5000"), params.toString());
        assertTrue(params.contains("customer_email=lea%40example.com"), params.toString());
        assertFalse(params.stream().anyMatch(param -> param.startsWith("client_reference_id")), params.toString());
        assertFalse(params.stream().anyMatch(param -> param.startsWith("shipping_")), params.toString());
        assertFalse(params.stream().anyMatch(param -> param.startsWith("phone_number_collection")), params.toString());
        assertFalse(params.stream().anyMatch(param -> param.startsWith("discounts")), params.toString());
    }

    @Test
    void giftCardPartIsOneCouponWithTheCardInMetadataAndAnExpiry() {
        var params = resource.giftCardParams(
                new StripeCheckoutSessionResource.GiftCardDiscount(new BigDecimal("20.00"), "card-1"), "coupon_1", 1_700_000_000L);

        assertTrue(params.contains("discounts[0][coupon]=coupon_1"), params.toString());
        assertTrue(params.contains("metadata[used_gift_card_id]=card-1"), params.toString());
        assertTrue(params.contains("metadata[used_gift_card_amount]=2000"), params.toString());
        assertTrue(params.contains("expires_at=1700000000"), params.toString());
        assertFalse(params.stream().anyMatch(param -> param.contains("promo_code")), params.toString());
    }

    @Test
    void sendsNothingAboutPromotionsWithoutADiscount() {
        var params = resource.buildParams(items, "cart-1", new BigDecimal("4.90"), null, null);

        assertFalse(params.stream().anyMatch(param -> param.startsWith("discounts") || param.startsWith("metadata")),
                params.toString());
    }

}
