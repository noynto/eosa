package me.noynto.eosa.infrastructure.fetch.stripe.resource;

import com.google.gson.annotations.SerializedName;
import me.noynto.eosa.infrastructure.fetch.stripe.config.StripeHttpClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public record StripeCheckoutSessionResource(StripeHttpClient http) {

    public CheckoutSession post(List<LineItem> lineItems, String clientReferenceId, BigDecimal shippingAmount) throws IOException, InterruptedException {
        return post(lineItems, clientReferenceId, shippingAmount, null);
    }

    /**
     * Crée la session. Avec {@code discount}, le site crée d'abord chez Stripe un coupon à usage unique du montant de
     * la remise : Stripe n'a aucune notion de code de promotion, il ne reçoit que la valeur à déduire. Le coupon se
     * répartit sur les lignes, la livraison n'est pas remisée. Le code est posé en métadonnées, relues à la commande.
     */
    public CheckoutSession post(List<LineItem> lineItems, String clientReferenceId, BigDecimal shippingAmount,
                                Discount discount) throws IOException, InterruptedException {
        String couponId = discount == null ? null : createCoupon(discount);
        return http.post("/v1/checkout/sessions", String.join("&", buildParams(lineItems, clientReferenceId, shippingAmount, discount, couponId)),
                CheckoutSession.class);
    }

    /** Coupon d'un montant fixe, valable une seule fois : il ne sert qu'à cette session. */
    String createCoupon(Discount discount) throws IOException, InterruptedException {
        String body = "amount_off=" + StripeHttpClient.toStripeAmount(discount.amount())
                + "&currency=eur&duration=once&max_redemptions=1&name=" + StripeHttpClient.encode(discount.code());
        Coupon coupon = http.post("/v1/coupons", body, Coupon.class);
        if (coupon == null || coupon.id() == null) {
            throw new IOException("Stripe n'a pas créé le coupon de la remise.");
        }
        return coupon.id();
    }

    List<String> buildParams(List<LineItem> lineItems, String clientReferenceId, BigDecimal shippingAmount,
                                    Discount discount, String couponId) {
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
        // Mondial Relay livre à domicile dans ces trois pays ; la Suisse est refusée à domicile (et demanderait la douane).
        String[] countries = {"FR", "BE", "LU"};
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
        if (couponId != null) {
            params.add("discounts[0][coupon]=" + StripeHttpClient.encode(couponId));
            params.add("metadata[promo_code_id]=" + StripeHttpClient.encode(discount.promoCodeId()));
            params.add("metadata[promo_code]=" + StripeHttpClient.encode(discount.code()));
        }
        return params;
    }

    public CheckoutSession retrieveSession(String sessionId) throws IOException, InterruptedException {
        return http.get("/v1/checkout/sessions/" + sessionId, CheckoutSession.class);
    }

    /**
     * Une page de sessions créées à partir de {@code createdSince} (secondes depuis 1970), les plus récentes d'abord ;
     * {@code startingAfter} est le dernier identifiant de la page précédente, null pour la première.
     */
    public SessionList listSessions(long createdSince, String startingAfter) throws IOException, InterruptedException {
        String path = "/v1/checkout/sessions?limit=100&created[gte]=" + createdSince
                + (startingAfter != null ? "&starting_after=" + StripeHttpClient.encode(startingAfter) : "");
        SessionList list = http.get(path, SessionList.class);
        if (list == null || list.data() == null) {
            throw new IOException("Réponse inattendue de Stripe pour la liste des sessions.");
        }
        return list;
    }

    public LineItemList retrieveLineItems(String sessionId) throws IOException, InterruptedException {
        return http.get("/v1/checkout/sessions/" + sessionId + "/line_items?expand[]=data.price.product", LineItemList.class);
    }

    /** La remise d'un code de promotion : son montant, et le code pour le retrouver à la commande. */
    public record Discount(
            BigDecimal amount,
            String promoCodeId,
            String code
    ) {}

    public record Coupon(
            String id
    ) {}

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
            @SerializedName("collected_information") CollectedInformation collectedInformation,
            @SerializedName("total_details") TotalDetails totalDetails,
            java.util.Map<String, String> metadata
    ) {

        /** Valeur d'une métadonnée de la session (posée à sa création), ou null. */
        public String metadata(String key) {
            return metadata == null ? null : metadata.get(key);
        }

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

    public record TotalDetails(
            @SerializedName("amount_discount") Long amountDiscount
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
            @SerializedName("amount_subtotal") Long amountSubtotal,
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

    public record SessionList(
            List<CheckoutSession> data,
            @SerializedName("has_more") boolean hasMore
    ) {}

    public record LineItemList(
            List<PurchasedItem> data,
            @SerializedName("has_more") boolean hasMore
    ) {}

}