package me.noynto.eosa;

import java.net.URI;
import java.net.URISyntaxException;

public class Configuration {

    private static final String BASE_URL = "EOSA_BASE_URL";
    private static final String ADMIN_NAME = "EOSA_ADMIN_NAME";
    private static final String ADMIN_SECRET = "EOSA_ADMIN_SECRET";

    public static Properties getProperties() {
        URI baseUri;
        try {
            String baseUrl = System.getenv(BASE_URL);
            if (baseUrl == null) {
                throw new MissingProperties("L'url de base du serveur est requis.");
            }
            baseUri = new URI(baseUrl);
        } catch (URISyntaxException e) {
            throw new InvalidProperties("L'url de base du serveur est invalide.", e);
        }

        String adminName = System.getenv(ADMIN_NAME);
        if (adminName == null) {
            throw new MissingProperties("Le nom de l'administrateur par défaut est requis.");
        }
        String adminSecret = System.getenv(ADMIN_SECRET);
        if (adminSecret == null) {
            throw new MissingProperties("Le secret de l'administrateur par défaut est requis.");
        }
        return new Properties(
                baseUri,
                adminName,
                adminSecret
        );
    }

    public static class MissingProperties extends RuntimeException {
        public MissingProperties(String message) {
            super(message);
        }
    }

    public static class InvalidProperties extends RuntimeException {
        public InvalidProperties(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
