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
            // Relus à la lecture de la session pour relier la ligne de commande à son bijou et à son image.
            if (item.jewelId() != null) {
                params.add(prefix + "[price_data][product_data][metadata][jewel_id]=" + StripeHttpClient.encode(item.jewelId()));
            }
            if (item.imageId() != null) {
                params.add(prefix + "[price_data][product_data][metadata][image_id]=" + StripeHttpClient.encode(item.imageId()));
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
        return http.get("/v1/checkout/sessions/" + sessionId + "/line_items?expand[]=data.price.product", LineItemList.class);
    }

    public record LineItem(
            String name,
            BigDecimal amount,
            int quantity,
            String imageUrl,
            String jewelId,
            String imageId
    ) {}

    public record CheckoutSession(
            String id,
            String url,
            String status,
            @SerializedName("payment_status") String paymentStatus,
            @SerializedName("customer_details") CustomerDetails customerDetails,
            @SerializedName("client_reference_id") String clientReferenceId,
            @SerializedName("amount_total") Long amountTotal,
            @SerializedName("shipping_cost") ShippingCost shippingCost,
            @SerializedName("shipping_details") ShippingDetails shippingDetails,
            @SerializedName("collected_information") CollectedInformation collectedInformation
    ) {

        /**
         * Adresse de livraison : selon la version de l'API du compte Stripe, elle est dans
         * {@code shipping_details} ou dans {@code collected_information.shipping_details}.
         */
        public ShippingDetails shippingDetailsOrCollected() {
            if (shippingDetails != null) {
                return shippingDetails;
            }
            return collectedInformation != null ? collectedInformation.shippingDetails() : null;
        }
    }

    public record CustomerDetails(
            String email,
            String name,
            String phone
    ) {}

    public record ShippingCost(
            @SerializedName("amount_total") Long amountTotal
    ) {}

    public record CollectedInformation(
            @SerializedName("shipping_details") ShippingDetails shippingDetails
    ) {}

    public record ShippingDetails(
            String name,
            Address address
    ) {}

    public record Address(
            String line1,
            String line2,
            @SerializedName("postal_code") String postalCode,
            String city,
            String country
    ) {}

    public record PurchasedItem(
            String description,
            long quantity,
            @SerializedName("amount_total") long amountTotal,
            Price price
    ) {

        /**
         * Métadonnée du produit de la ligne (présente quand le produit est développé), ou null.
         */
        public String productMetadata(String key) {
            if (price == null || price.product() == null || !price.product().isJsonObject()) {
                return null;
            }
            var metadata = price.product().getAsJsonObject().get("metadata");
            if (metadata == null || !metadata.isJsonObject()) {
                return null;
            }
            var value = metadata.getAsJsonObject().get(key);
            return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
        }
    }

    /**
     * {@code product} est un identifiant, ou un objet quand la requête le développe : on le garde brut pour ne
     * jamais échouer à la lecture d'une session.
     */
    public record Price(
            com.google.gson.JsonElement product
    ) {}

    public record LineItemList(
            List<PurchasedItem> data,
            @SerializedName("has_more") boolean hasMore
    ) {}

}