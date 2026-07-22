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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SetDefaultVariantOfProductTest {

    @Mock ProductProvider productProvider;

    @Test
    void handle_setsDefaultVariant() {
        var productId = new ProductId("abc");
        var variantId = new VariantId("v1");
        var product = productWithVariant(productId, variantId);
        when(productProvider.read(productId)).thenReturn(Optional.of(product));
        when(productProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = new SetDefaultVariantOfProduct(productProvider).handle(
                new SetDefaultVariantOfProduct.Command(productId, variantId)
        );

        assertEquals(variantId, result.getDefaultVariantId());
    }

    @Test
    void handle_throwsWhenVariantNotFoundOnProduct() {
        var productId = new ProductId("abc");
        var product = productWithVariant(productId, new VariantId("v1"));
        when(productProvider.read(productId)).thenReturn(Optional.of(product));

        assertThrows(RuntimeException.class, () ->
                new SetDefaultVariantOfProduct(productProvider).handle(
                        new SetDefaultVariantOfProduct.Command(productId, new VariantId("unknown"))
                )
        );
    }

    @Test
    void handle_throwsWhenProductNotFound() {
        var productId = new ProductId("unknown");
        when(productProvider.read(productId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new SetDefaultVariantOfProduct(productProvider).handle(
                        new SetDefaultVariantOfProduct.Command(productId, new VariantId("v1"))
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
