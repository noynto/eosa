package me.noynto.eosa.shipping;

/**
 * Destinataire d'un colis, copié de la commande pour créer l'étiquette. Il n'est jamais stocké avec l'étiquette :
 * l'adresse reste dans la commande, où elle est effacée au terme de sa conservation.
 */
public record ShippingRecipient(
        String name,
        String email,
        String phone,
        String line1,
        String line2,
        String postalCode,
        String city,
        String country
) {
}
