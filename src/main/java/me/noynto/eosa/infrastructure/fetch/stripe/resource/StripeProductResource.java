package me.noynto.eosa.infrastructure.fetch.stripe.resource;

import com.google.gson.annotations.SerializedName;
import me.noynto.eosa.infrastructure.fetch.stripe.config.StripeHttpClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record StripeProductResource(
        StripeHttpClient http
) {

    // -------------------------------------------------------------------------
    // Products
    // -------------------------------------------------------------------------

    public Product createProduct(
            String name,
            String internalProductId
    ) throws IOException, InterruptedException {
        String body = "name=" + StripeHttpClient.encode(name)
                + "&metadata[product_id]=" + StripeHttpClient.encode(internalProductId);
        return http.post("/v1/products", body, Product.class);
    }

    public Product retrieveProduct(String stripeProductId) throws IOException, InterruptedException {
        return http.get("/v1/products/" + stripeProductId, Product.class);
    }

    public Product updateProduct(
            String stripeProductId,
            String name
    ) throws IOException, InterruptedException {
        return http.post("/v1/products/" + stripeProductId, "name=" + StripeHttpClient.encode(name), Product.class);
    }

    public void deleteProduct(String stripeProductId) throws IOException, InterruptedException {
        http.delete("/v1/products/" + stripeProductId);
    }

    public Optional<Product> findProduct(String internalProductId) throws IOException, InterruptedException {
        String path = "/v1/products?metadata[product_id]=" + StripeHttpClient.encode(internalProductId) + "&limit=1";
        List<Product> data = http.get(path, ProductList.class).data();
        return data == null || data.isEmpty() ? Optional.empty() : Optional.of(data.getFirst());
    }

    // -------------------------------------------------------------------------
    // Prices
    // -------------------------------------------------------------------------

    public Price createPrice(
            String stripeProductId,
            BigDecimal unitPrice
    ) throws IOException, InterruptedException {
        String body = "currency=eur"
                + "&unit_amount=" + StripeHttpClient.toStripeAmount(unitPrice)
                + "&product=" + StripeHttpClient.encode(stripeProductId);
        return http.post("/v1/prices", body, Price.class);
    }

    public Price retrievePrice(String stripePriceId) throws IOException, InterruptedException {
        return http.get("/v1/prices/" + stripePriceId, Price.class);
    }

    public Price archivePrice(String stripePriceId) throws IOException, InterruptedException {
        return http.post("/v1/prices/" + stripePriceId, "active=false", Price.class);
    }

    public Optional<Price> findPrice(
            String stripeProductId,
            BigDecimal unitPrice
    ) throws IOException, InterruptedException {
        String path = "/v1/prices?product=" + stripeProductId + "&active=true&currency=eur&limit=10";
        List<Price> data = http.get(path, PriceList.class).data();
        if (data == null) return Optional.empty();
        long amount = StripeHttpClient.toStripeAmount(unitPrice);
        return data.stream().filter(p -> p.unitAmount() == amount).findFirst();
    }

    // -------------------------------------------------------------------------
    // DTOs
    // -------------------------------------------------------------------------

    public record Product(
            String id,
            String name,
            boolean active,
            String description,
            Map<String, String> metadata
    ) {}

    public record ProductList(
            List<Product> data,
            @SerializedName("has_more") boolean hasMore
    ) {}

    public record Price(
            String id,
            @SerializedName("unit_amount") long unitAmount,
            boolean active,
            String currency,
            String product
    ) {}

    public record PriceList(
            List<Price> data,
            @SerializedName("has_more") boolean hasMore
    ) {}

}