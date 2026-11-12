package me.noynto.eosa.infrastructure.fetch.mondialrelay.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

public class MondialRelayConnectConfiguration {

    static final String SANDBOX_ENDPOINT = "https://connect-api-sandbox.mondialrelay.com/api/shipment";

    public static HttpClient getClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public static MondialRelayConnectProperties getProperties() {
        return new MondialRelayConnectProperties(
                URI.create(env("EOSA_MONDIAL_RELAY_API_URL", SANDBOX_ENDPOINT)),
                env("EOSA_MONDIAL_RELAY_LOGIN", null),
                env("EOSA_MONDIAL_RELAY_PASSWORD", null),
                env("EOSA_MONDIAL_RELAY_CUSTOMER_ID", null),
                new MondialRelayConnectProperties.Sender(
                        env("EOSA_SHIPPING_SENDER_NAME", null),
                        env("EOSA_SHIPPING_SENDER_LINE1", null),
                        env("EOSA_SHIPPING_SENDER_POSTAL_CODE", null),
                        env("EOSA_SHIPPING_SENDER_CITY", null),
                        env("EOSA_SHIPPING_SENDER_PHONE", null),
                        env("EOSA_SHIPPING_SENDER_EMAIL", null)
                )
        );
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value.strip();
    }

}
