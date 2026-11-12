package me.noynto.eosa.infrastructure.fetch.mondialrelay.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

public class MondialRelayConnectConfiguration {

    static final String TRACKING_ENDPOINT = "https://api.mondialrelay.com/WebService.asmx";

    static final String SANDBOX_ENDPOINT = "https://connect-api-sandbox.mondialrelay.com/api/shipment";

    public static HttpClient getClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public static MondialRelayConnectProperties getProperties() {
        return new MondialRelayConnectProperties(
                URI.create(env("EOSA_CLIENT_MONDIAL_RELAY_V2_API_URL", SANDBOX_ENDPOINT)),
                env("EOSA_CLIENT_MONDIAL_RELAY_V2_LOGIN", null),
                env("EOSA_CLIENT_MONDIAL_RELAY_V2_PASSWORD", null),
                env("EOSA_CLIENT_MONDIAL_RELAY_V2_CUSTOMER_ID", null),
                new MondialRelayConnectProperties.Sender(
                        env("EOSA_SHIPPING_SENDER_NAME", null),
                        env("EOSA_SHIPPING_SENDER_LINE1", null),
                        env("EOSA_SHIPPING_SENDER_POSTAL_CODE", null),
                        env("EOSA_SHIPPING_SENDER_CITY", null),
                        env("EOSA_SHIPPING_SENDER_PHONE", null),
                        env("EOSA_SHIPPING_SENDER_EMAIL", null)
                ),
                new MondialRelayConnectProperties.Parcel(
                        centimeters("EOSA_SHIPPING_PARCEL_LENGTH_CM", 12),
                        centimeters("EOSA_SHIPPING_PARCEL_WIDTH_CM", 9),
                        centimeters("EOSA_SHIPPING_PARCEL_DEPTH_CM", 4)
                )
        );
    }

    public static MondialRelayTrackingProperties getTrackingProperties() {
        return new MondialRelayTrackingProperties(
                URI.create(env("EOSA_CLIENT_MONDIAL_RELAY_V1_API_URL", TRACKING_ENDPOINT)),
                env("EOSA_CLIENT_MONDIAL_RELAY_V1_ENSEIGNE", null),
                env("EOSA_CLIENT_MONDIAL_RELAY_V1_PRIVATE_KEY", null)
        );
    }

    private static int centimeters(String name, int defaultValue) {
        String value = env(name, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new NumberFormatException(value);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " doit être un nombre entier de centimètres positif.", e);
        }
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value.strip();
    }

}
