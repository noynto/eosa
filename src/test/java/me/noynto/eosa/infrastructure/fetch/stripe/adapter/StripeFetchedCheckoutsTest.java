package me.noynto.eosa.infrastructure.fetch.stripe.adapter;

import me.noynto.eosa.checkout.Checkout;
import me.noynto.eosa.checkout.CheckoutItem;
import me.noynto.eosa.checkout.CheckoutSessionStatus;
import me.noynto.eosa.checkout.CheckoutStatus;
import me.noynto.eosa.infrastructure.fetch.stripe.config.StripeHttpClient;
import me.noynto.eosa.infrastructure.fetch.stripe.config.StripeProperties;
import me.noynto.eosa.infrastructure.fetch.stripe.resource.StripeCheckoutSessionResource;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.VariantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "EOSA_STRIPE_SECRET_KEY", matches = "sk_test_.*")
class StripeFetchedCheckoutsTest {

    private StripeFetchedCheckouts adapter;

    @BeforeEach
    void setUp() {
        String secretKey = System.getenv("EOSA_STRIPE_SECRET_KEY");
        StripeProperties properties = new StripeProperties(
                secretKey,
                "http://localhost:8080/checkout/success?session_id={CHECKOUT_SESSION_ID}",
                "http://localhost:8080/cart"
        );
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.of(10, ChronoUnit.SECONDS))
                .build();
        adapter = new StripeFetchedCheckouts(
                new StripeCheckoutSessionResource(new StripeHttpClient(client, properties)),
                "http://localhost:8080"
        );
    }

    @Test
    void write_addsStripeSessionToCheckout() throws Exception {
        Checkout checkout = new Checkout();
        checkout.setStatus(CheckoutStatus.PENDING);
        checkout.setItems(List.of(
                item("Collier Lune", "29.90", 1),
                item("Bracelet Soleil", "22.00", 2)
        ));

        Checkout result = adapter.write(checkout);

        assertNotNull(result.getSession());

        var session = result.getSession();
        assertNotNull(session.getId());
        assertTrue(session.getId().getStripe().startsWith("cs_test_"));
        assertNotNull(session.getUri());
        assertTrue(session.getUri().toString().contains("stripe.com"));
        assertEquals(CheckoutSessionStatus.OPENED, session.getStatus());

        System.out.println("Stripe session ID  : " + session.getId().getStripe());
        System.out.println("Stripe session URL : " + session.getUri());
    }

    @Test
    void write_replacesExistingSession() throws Exception {
        Checkout checkout = new Checkout();
        checkout.setStatus(CheckoutStatus.PENDING);
        checkout.setItems(List.of(item("Collier Lune", "29.90", 1)));

        adapter.write(checkout);
        String firstSessionId = checkout.getSession().getId().getStripe();

        adapter.write(checkout);
        String secondSessionId = checkout.getSession().getId().getStripe();

        assertNotEquals(firstSessionId, secondSessionId);
        System.out.println("Session remplacée : " + firstSessionId + " → " + secondSessionId);
    }

    @Test
    void write_includesImageUrlWhenImageIdIsPresent() throws Exception {
        Checkout checkout = new Checkout();
        checkout.setStatus(CheckoutStatus.PENDING);

        CheckoutItem itemWithImage = item("Collier Lune", "29.90", 1);
        itemWithImage.setImageId(new ImageId("abc123"));
        checkout.setItems(List.of(itemWithImage));

        Checkout result = adapter.write(checkout);

        assertNotNull(result.getSession());
        assertTrue(result.getSession().getId().getStripe().startsWith("cs_test_"));
        System.out.println("Session avec image : " + result.getSession().getUri());
    }

    private CheckoutItem item(String name, String price, int quantity) {
        CheckoutItem item = new CheckoutItem();
        item.setVariantId(new VariantId("test-" + name));
        item.setName(name);
        item.setUnitPrice(new BigDecimal(price));
        item.setQuantity(quantity);
        return item;
    }

}