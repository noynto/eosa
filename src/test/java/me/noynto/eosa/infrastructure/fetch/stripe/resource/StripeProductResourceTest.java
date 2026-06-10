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

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "EOSA_STRIPE_SECRET_KEY", matches = "sk_test_.*")
class StripeProductResourceTest {

    private StripeProductResource resource;

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
        resource = new StripeProductResource(new StripeHttpClient(client, properties));
    }

    @Test
    void createProduct_returnsProductWithId() throws Exception {
        var product = resource.createProduct("Collier Test", "test-internal-id-001");

        assertNotNull(product.id());
        assertFalse(product.id().isBlank());
        assertEquals("Collier Test", product.name());
        System.out.println("Product ID   : " + product.id());
        System.out.println("Product name : " + product.name());
    }

    @Test
    void findProduct_returnsCreatedProduct() throws Exception {
        resource.createProduct("Bracelet Test", "test-internal-id-002");

        var found = resource.findProduct("test-internal-id-002");

        assertTrue(found.isPresent());
        assertEquals("Bracelet Test", found.get().name());
        System.out.println("Found ID   : " + found.get().id());
        System.out.println("Found name : " + found.get().name());
    }

    @Test
    void createPrice_returnsPrice() throws Exception {
        var product = resource.createProduct("Produit Prix Test", "test-internal-id-003");

        var price = resource.createPrice(product.id(), new BigDecimal("19.90"));

        assertNotNull(price.id());
        assertEquals(1990L, price.unitAmount());
        System.out.println("Price ID     : " + price.id());
        System.out.println("Unit amount  : " + price.unitAmount());
    }

    @Test
    void findPrice_returnsCreatedPrice() throws Exception {
        var product = resource.createProduct("Produit Prix Recherche", "test-internal-id-004");
        resource.createPrice(product.id(), new BigDecimal("34.50"));

        var found = resource.findPrice(product.id(), new BigDecimal("34.50"));

        assertTrue(found.isPresent());
        assertEquals(3450L, found.get().unitAmount());
        System.out.println("Found price ID : " + found.get().id());
    }

}