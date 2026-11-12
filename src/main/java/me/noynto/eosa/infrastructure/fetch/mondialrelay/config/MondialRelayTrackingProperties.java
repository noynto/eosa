package me.noynto.eosa.infrastructure.fetch.mondialrelay.config;

import java.net.URI;

/**
 * Accès au web service V1 (SOAP) de Mondial Relay, utilisé pour lire le suivi d'un colis. Les champs peuvent être
 * vides : l'application démarre sans, et la lecture du suivi échoue alors avec un message clair.
 *
 * @param endpoint   adresse du web service, la même pour les essais et la production
 * @param enseigne   code enseigne de 8 caractères
 * @param privateKey clé privée qui signe chaque appel
 */
public record MondialRelayTrackingProperties(
        URI endpoint,
        String enseigne,
        String privateKey
) {

    public boolean isConfigured() {
        return enseigne != null && !enseigne.isBlank() && privateKey != null && !privateKey.isBlank();
    }

}
