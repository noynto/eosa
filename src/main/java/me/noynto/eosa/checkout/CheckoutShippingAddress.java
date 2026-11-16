package me.noynto.eosa.checkout;

/**
 * Adresse de livraison saisie par la cliente chez Stripe.
 */
public record CheckoutShippingAddress(
        String name,
        String line1,
        String line2,
        String postalCode,
        String city,
        String country
) {
}
