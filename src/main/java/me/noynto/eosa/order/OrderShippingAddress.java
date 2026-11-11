package me.noynto.eosa.order;

/**
 * Adresse de livraison saisie chez Stripe, copiée dans la commande.
 */
public record OrderShippingAddress(
        String name,
        String line1,
        String line2,
        String postalCode,
        String city,
        String country
) {
}
