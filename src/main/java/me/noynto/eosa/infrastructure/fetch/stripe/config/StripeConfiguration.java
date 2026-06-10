package me.noynto.eosa.infrastructure.fetch.stripe.config;

import me.noynto.eosa.Properties;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class StripeConfiguration {

    private static final String SECRET = "EOSA_CLIENT_STRIPE_SECRET_KEY";

    public static HttpClient getClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.of(10, ChronoUnit.SECONDS))
                .build();
    }

    public static StripeProperties getProperties(Properties properties) {
        String secretKey = Objects.requireNonNull(System.getenv(SECRET), "La clé secrète Stripe est obligatoire.");
        String baseUrl = properties.baseUrl().toString();
        return new StripeProperties(
                secretKey,
                baseUrl + "/checkout/success?session_id={CHECKOUT_SESSION_ID}",
                baseUrl + "/cart"
        );
    }

}