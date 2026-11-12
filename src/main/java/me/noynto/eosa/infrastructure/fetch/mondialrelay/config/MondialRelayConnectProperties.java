package me.noynto.eosa.infrastructure.fetch.mondialrelay.config;

import java.net.URI;

/**
 * Accès à l'API Connect de Mondial Relay (création d'étiquettes) et adresse d'expédition imprimée sur chaque
 * étiquette. Les champs peuvent être vides : l'application démarre sans, et la fabrication d'une commande échoue
 * alors avec un message clair.
 *
 * @param endpoint   adresse de l'API : le bac à sable par défaut, la production quand on la configure
 * @param login      identifiant de l'API (de la forme {@code CODE@business-api.mondialrelay.com})
 * @param password   mot de passe de l'API
 * @param customerId code client de 8 caractères
 */
public record MondialRelayConnectProperties(
        URI endpoint,
        String login,
        String password,
        String customerId,
        Sender sender,
        Parcel parcel
) {

    public boolean isConfigured() {
        return filled(login) && filled(password) && filled(customerId)
                && filled(sender.name()) && filled(sender.line1()) && filled(sender.postalCode())
                && filled(sender.city());
    }

    private static boolean filled(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Dimensions du colis en centimètres : le transporteur les exige (erreur {@code WebApi_MandatoryParcelSize}
     * sans elles) et aucun bijou n'a de dimensions dans le modèle, donc la boîte d'expédition est la même pour tous.
     */
    public record Parcel(
            int lengthCm,
            int widthCm,
            int depthCm
    ) {
    }

    /**
     * Adresse de l'expéditeur (EOSA). Le téléphone et l'e-mail sont facultatifs.
     */
    public record Sender(
            String name,
            String line1,
            String postalCode,
            String city,
            String phone,
            String email
    ) {
    }

}
