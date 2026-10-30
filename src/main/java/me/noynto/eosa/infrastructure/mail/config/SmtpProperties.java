package me.noynto.eosa.infrastructure.mail.config;

/**
 * @param username absent pour un serveur sans authentification (Mailpit en local)
 */
public record SmtpProperties(
        String host,
        int port,
        Security security,
        String username,
        String password,
        String from
) {

    public enum Security {
        /** TLS dès la connexion (port 465). */
        SSL,
        /** Connexion en clair puis passage obligatoire en TLS (port 587). */
        STARTTLS,
        /** Aucun chiffrement : uniquement pour un serveur local de test. */
        NONE
    }

}
