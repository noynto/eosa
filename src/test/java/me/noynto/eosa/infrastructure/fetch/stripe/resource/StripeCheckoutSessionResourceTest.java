package me.noynto.eosa.infrastructure.fetch.stripe.resource;

import me.noynto.eosa.infrastructure.fetch.stripe.config.StripeHttpClient;
import me.noynto.eosa.infrastructure.fetch.stripe.config.StripeProperties;
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
class StripeCheckoutSessionResourceTest {

    private StripeCheckoutSessionResource resource;

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
        resource = new StripeCheckoutSessionResource(new StripeHttpClient(client, properties));
    }

    @Test
    void postWithIdAndUrl() throws Exception {
        var items = List.of(
                new StripeCheckoutSessionResource.LineItem("Collier Lune", new BigDecimal("29.90"), 1, null),
                new StripeCheckoutSessionResource.LineItem("Bracelet Soleil", new BigDecimal("22.00"), 2, null)
        );

        var session = resource.post(items, null, null);

        assertNotNull(session.id());
        assertTrue(session.id().startsWith("cs_test_"));
        assertNotNull(session.url());
        assertTrue(session.url().contains("stripe.com"));
        System.out.println("Session ID  : " + session.id());
        System.out.println("Session URL : " + session.url());
    }

    @Test
    void retrieveSession_returnsCreatedSession() throws Exception {
        var items = List.of(
                new StripeCheckoutSessionResource.LineItem("Collier Lune", new BigDecimal("29.90"), 1, null)
        );
        var created = resource.post(items, null, null);

        var retrieved = resource.retrieveSession(created.id());

        assertNotNull(retrieved.id());
        assertEquals(created.id(), retrieved.id());
        assertEquals("unpaid", retrieved.paymentStatus());
        System.out.println("Payment status : " + retrieved.paymentStatus());
    }

    @Test
    void retrieveLineItems_returnsItemsOfCreatedSession() throws Exception {
        var items = List.of(
                new StripeCheckoutSessionResource.LineItem("Collier Lune", new BigDecimal("29.90"), 1, null),
                new StripeCheckoutSessionResource.LineItem("Bracelet Soleil", new BigDecimal("22.00"), 2, null)
        );
        var created = resource.post(items, null, null);

        var lineItems = resource.retrieveLineItems(created.id());

        assertNotNull(lineItems);
        assertNotNull(lineItems.data());
        assertEquals(2, lineItems.data().size());
        lineItems.data().forEach(item -> {
            assertNotNull(item.description());
            assertTrue(item.quantity() > 0);
            assertTrue(item.amountTotal() > 0);
            System.out.println(item.description() + " × " + item.quantity() + " = " + item.amountTotal() + " cts");
        });
    }

}