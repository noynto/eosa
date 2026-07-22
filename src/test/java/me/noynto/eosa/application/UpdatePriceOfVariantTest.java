package me.noynto.eosa.application;

import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.product.Variant;
import me.noynto.eosa.shared.ProductId;
import me.noynto.eosa.shared.VariantId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdatePriceOfVariantTest {

    @Mock ProductProvider productProvider;

    @Test
    void handle_updatesVariantPrice() {
        var productId = new ProductId("abc");
        var variantId = new VariantId("v1");
        var product = productWithVariant(productId, variantId);
        when(productProvider.read(productId)).thenReturn(Optional.of(product));
        when(productProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = new UpdatePriceOfVariant(productProvider).handle(
                new UpdatePriceOfVariant.Command(productId, variantId, new BigDecimal("29.90"))
        );

        assertEquals(new BigDecimal("29.90"), result.getVariants().getFirst().getPrice());
    }

    @Test
    void handle_throwsWhenPriceIsZeroOrNegative() {
        assertThrows(RuntimeException.class, () ->
                new UpdatePriceOfVariant(productProvider).handle(
                        new UpdatePriceOfVariant.Command(new ProductId("abc"), new VariantId("v1"), BigDecimal.ZERO)
                )
        );
    }

    @Test
    void handle_throwsWhenVariantNotFoundOnProduct() {
        var productId = new ProductId("abc");
        var product = productWithVariant(productId, new VariantId("v1"));
        when(productProvider.read(productId)).thenReturn(Optional.of(product));

        assertThrows(RuntimeException.class, () ->
                new UpdatePriceOfVariant(productProvider).handle(
                        new UpdatePriceOfVariant.Command(productId, new VariantId("unknown"), new BigDecimal("10"))
                )
        );
    }

    @Test
    void handle_throwsWhenProductNotFound() {
        var productId = new ProductId("unknown");
        when(productProvider.read(productId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new UpdatePriceOfVariant(productProvider).handle(
                        new UpdatePriceOfVariant.Command(productId, new VariantId("v1"), new BigDecimal("10"))
                )
        );
    }

    private Product productWithVariant(ProductId productId, VariantId variantId) {
        var product = new Product();
        product.setId(productId);
        var variant = new Variant();
        variant.setId(variantId);
        product.setVariants(List.of(variant));
        return product;
    }

}
