package me.noynto.eosa.infrastructure.fetch.stripe.config;

import com.google.gson.Gson;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public record StripeHttpClient(
        HttpClient client,
        StripeProperties properties
) {

    private static final URI STRIPE_BASE = URI.create("https://api.stripe.com");
    private static final Gson GSON = new Gson();

    public <T> T get(String path, Class<T> type) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(STRIPE_BASE.resolve(path))
                .header("Authorization", auth())
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return GSON.fromJson(response.body(), type);
    }

    public <T> T post(String path, String body, Class<T> type) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(STRIPE_BASE.resolve(path))
                .header("Authorization", auth())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return GSON.fromJson(response.body(), type);
    }

    public void delete(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(STRIPE_BASE.resolve(path))
                .header("Authorization", auth())
                .DELETE()
                .build();
        client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static long toStripeAmount(BigDecimal price) {
        return price.multiply(BigDecimal.valueOf(100)).longValue();
    }

    private String auth() {
        return "Basic " + Base64.getEncoder().encodeToString(
                (properties.secretKey() + ":").getBytes(StandardCharsets.UTF_8));
    }

}