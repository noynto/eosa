package me.noynto.eosa.infrastructure.fetch.stripe.resource;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Lecture du bijou d'une ligne Stripe, sur des réponses JSON d'exemple (sans appel réseau).
 */
class StripeLineItemMetadataTest {

    private StripeCheckoutSessionResource.PurchasedItem item(String json) {
        return new Gson().fromJson(json, StripeCheckoutSessionResource.PurchasedItem.class);
    }

    @Test
    void readsTheJewelAndImageOfAnExpandedProduct() {
        var item = item("""
                {"description": "Collier Lune", "quantity": 1, "amount_total": 3300,
                 "price": {"product": {"id": "prod_1", "metadata": {"jewel_id": "j-1", "image_id": "i-1"}}}}
                """);

        assertEquals("j-1", item.productMetadata("jewel_id"));
        assertEquals("i-1", item.productMetadata("image_id"));
    }

    @Test
    void staysEmptyWhenTheProductIsNotExpandedOrHasNoMetadata() {
        var notExpanded = item("""
                {"description": "Collier Lune", "quantity": 1, "amount_total": 3300, "price": {"product": "prod_1"}}
                """);
        var noMetadata = item("""
                {"description": "Collier Lune", "quantity": 1, "amount_total": 3300,
                 "price": {"product": {"id": "prod_1", "metadata": {}}}}
                """);
        var noPrice = item("""
                {"description": "Collier Lune", "quantity": 1, "amount_total": 3300}
                """);

        assertNull(notExpanded.productMetadata("jewel_id"));
        assertNull(noMetadata.productMetadata("jewel_id"));
        assertNull(noPrice.productMetadata("jewel_id"));
    }

}
