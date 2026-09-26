package me.noynto.eosa;

import java.net.URI;
import java.net.URISyntaxException;

public class Configuration {

    private static final String PUBLIC_BASE_URL = "EOSA_PUBLIC_BASE_URL";
    private static final String PUBLIC_SERVER_PORT = "EOSA_PUBLIC_SERVER_PORT";
    private static final String ADMIN_NAME = "EOSA_ADMIN_NAME";
    private static final String ADMIN_SECRET = "EOSA_ADMIN_SECRET";
    private static final String ADMIN_BASE_URL = "EOSA_ADMIN_BASE_URL";
    private static final String ADMIN_SERVER_PORT = "EOSA_ADMIN_SERVER_PORT";
    private static final String DEFAULT_PUBLIC_SERVER_PORT = "8080";
    private static final String DEFAULT_ADMIN_SERVER_PORT = "18080";

    public static Properties getProperties() {
        URI publicBaseUri;
        try {
            String publicBaseUrl = System.getenv(PUBLIC_BASE_URL);
            if (publicBaseUrl == null) {
                throw new MissingProperties(
                    "L'url publique de base est requis."
                );
            }
            publicBaseUri = new URI(publicBaseUrl);
        } catch (URISyntaxException e) {
            throw new InvalidProperties(
                "L'url publique de base est invalide.",
                e
            );
        }
        int publicServerPort;
        try {
            publicServerPort = Integer.parseInt(
                System.getenv().getOrDefault(
                    PUBLIC_SERVER_PORT,
                    DEFAULT_PUBLIC_SERVER_PORT
                )
            );
        } catch (NumberFormatException e) {
            throw new InvalidProperties(
                "Le port du serveur publique est invalide.",
                e
            );
        }
        URI adminBaseUri;
        try {
            String adminBaseUrl = System.getenv(ADMIN_BASE_URL);
            if (adminBaseUrl == null) {
                throw new MissingProperties(
                    "L'url administrateur de base est requis."
                );
            }
            adminBaseUri = new URI(adminBaseUrl);
        } catch (URISyntaxException e) {
            throw new InvalidProperties(
                "L'url administrateur de base est invalide.",
                e
            );
        }
        int adminServerPort;
        try {
            adminServerPort = Integer.parseInt(
                System.getenv().getOrDefault(
                    ADMIN_SERVER_PORT,
                    DEFAULT_ADMIN_SERVER_PORT
                )
            );
        } catch (NumberFormatException e) {
            throw new InvalidProperties(
                "Le port du serveur d'administration est invalide.",
                e
            );
        }
        String adminName = System.getenv(ADMIN_NAME);
        if (adminName == null) {
            throw new MissingProperties(
                "Le nom de l'administrateur par défaut est requis."
            );
        }
        String adminSecret = System.getenv(ADMIN_SECRET);
        if (adminSecret == null) {
            throw new MissingProperties(
                "Le secret de l'administrateur par défaut est requis."
            );
        }
        return new Properties(
            publicBaseUri,
            publicServerPort,
            adminBaseUri,
            adminServerPort,
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
