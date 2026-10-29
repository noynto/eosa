package me.noynto.eosa.infrastructure.mail.config;

import java.util.Locale;
import java.util.Objects;

public class SmtpConfiguration {

    private static final String HOST = "EOSA_SMTP_HOST";
    private static final String PORT = "EOSA_SMTP_PORT";
    private static final String SECURITY = "EOSA_SMTP_SECURITY";
    private static final String USERNAME = "EOSA_SMTP_USERNAME";
    private static final String PASSWORD = "EOSA_SMTP_PASSWORD";
    private static final String FROM = "EOSA_MAIL_FROM";
    private static final String DEFAULT_PORT = "465";
    private static final String DEFAULT_SECURITY = "ssl";
    private static final String DEFAULT_FROM = "contact@eosa.me";

    public static SmtpProperties getProperties() {
        String host = Objects.requireNonNull(System.getenv(HOST), HOST + " est obligatoire.");
        int port;
        try {
            port = Integer.parseInt(System.getenv().getOrDefault(PORT, DEFAULT_PORT));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(PORT + " doit être un numéro de port.", e);
        }
        SmtpProperties.Security security;
        try {
            security = SmtpProperties.Security.valueOf(
                    System.getenv().getOrDefault(SECURITY, DEFAULT_SECURITY).toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(SECURITY + " doit valoir ssl, starttls ou none.", e);
        }
        String username = blankToNull(System.getenv(USERNAME));
        String password = blankToNull(System.getenv(PASSWORD));
        if (username != null && password == null) {
            throw new IllegalArgumentException(PASSWORD + " est obligatoire quand " + USERNAME + " est défini.");
        }
        return new SmtpProperties(
                host,
                port,
                security,
                username,
                password,
                System.getenv().getOrDefault(FROM, DEFAULT_FROM)
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

}
