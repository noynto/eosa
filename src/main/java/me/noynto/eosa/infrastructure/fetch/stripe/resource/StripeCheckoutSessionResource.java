package me.noynto.eosa.infrastructure.fetch.stripe.resource;

import com.google.gson.annotations.SerializedName;
import me.noynto.eosa.infrastructure.fetch.stripe.config.StripeHttpClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public record StripeCheckoutSessionResource(StripeHttpClient http) {

    public CheckoutSession post(List<LineItem> lineItems, String clientReferenceId, BigDecimal shippingAmount) throws IOException, InterruptedException {
        List<String> params = new ArrayList<>();
        params.add("mode=payment");
        params.add("success_url=" + StripeHttpClient.encode(http.properties().successPath()));
        params.add("cancel_url=" + StripeHttpClient.encode(http.properties().cancelPath()));
        for (int i = 0; i < lineItems.size(); i++) {
            LineItem item = lineItems.get(i);
            String prefix = "line_items[" + i + "]";
            params.add(prefix + "[price_data][currency]=eur");
            params.add(prefix + "[price_data][unit_amount]=" + StripeHttpClient.toStripeAmount(item.amount()));
            params.add(prefix + "[price_data][product_data][name]=" + StripeHttpClient.encode(item.name()));
            if (item.imageUrl() != null) {
                params.add(prefix + "[price_data][product_data][images][0]=" + StripeHttpClient.encode(item.imageUrl()));
            }
            params.add(prefix + "[quantity]=" + item.quantity());
        }
        if (clientReferenceId != null) {
            params.add("client_reference_id=" + StripeHttpClient.encode(clientReferenceId));
        }
        params.add("phone_number_collection[enabled]=true");
        String[] countries = {"FR", "BE", "CH", "LU"};
        for (int i = 0; i < countries.length; i++) {
            params.add("shipping_address_collection[allowed_countries][" + i + "]=" + countries[i]);
        }
        if (shippingAmount != null) {
            String displayName = shippingAmount.compareTo(BigDecimal.ZERO) == 0 ? "Livraison offerte" : "Livraison standard";
            params.add("shipping_options[0][shipping_rate_data][type]=fixed_amount");
            params.add("shipping_options[0][shipping_rate_data][display_name]=" + StripeHttpClient.encode(displayName));
            params.add("shipping_options[0][shipping_rate_data][fixed_amount][amount]=" + StripeHttpClient.toStripeAmount(shippingAmount));
            params.add("shipping_options[0][shipping_rate_data][fixed_amount][currency]=eur");
        }
        return http.post("/v1/checkout/sessions", String.join("&", params), CheckoutSession.class);
    }

    public CheckoutSession retrieveSession(String sessionId) throws IOException, InterruptedException {
        return http.get("/v1/checkout/sessions/" + sessionId, CheckoutSession.class);
    }

    public LineItemList retrieveLineItems(String sessionId) throws IOException, InterruptedException {
        return http.get("/v1/checkout/sessions/" + sessionId + "/line_items", LineItemList.class);
    }

    public record LineItem(
            String name,
            BigDecimal amount,
            int quantity,
            String imageUrl
    ) {}

    public record CheckoutSession(
            String id,
            String url,
            String status,
            @SerializedName("payment_status") String paymentStatus,
            @SerializedName("customer_details") CustomerDetails customerDetails,
            @SerializedName("client_reference_id") String clientReferenceId
    ) {}

    public record CustomerDetails(
            String email,
            String name
    ) {}

    public record PurchasedItem(
            String description,
            long quantity,
            @SerializedName("amount_total") long amountTotal
    ) {}

    public record LineItemList(
            List<PurchasedItem> data,
            @SerializedName("has_more") boolean hasMore
    ) {}

}