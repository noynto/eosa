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
    void sendsNothingAboutPromotionsWithoutADiscount() {
        var params = resource.buildParams(items, "cart-1", new BigDecimal("4.90"), null, null);

        assertFalse(params.stream().anyMatch(param -> param.startsWith("discounts") || param.startsWith("metadata")),
                params.toString());
    }

}
