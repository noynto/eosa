package me.noynto.eosa.application;

import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.product.Variant;
import me.noynto.eosa.product.VariantState;
import me.noynto.eosa.shared.ImageId;
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
class UpdateStateOfVariantTest {

    @Mock ProductProvider productProvider;

    @Test
    void handle_publishesCompleteVariant() {
        var productId = new ProductId("abc");
        var product = productWithCompleteVariant(productId, VariantState.DRAFTED);
        when(productProvider.read(productId)).thenReturn(Optional.of(product));
        when(productProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = new UpdateStateOfVariant(productProvider).handle(
                new UpdateStateOfVariant.Command(productId, product.getVariants().getFirst().getId(), VariantState.PUBLISHED)
        );

        assertEquals(VariantState.PUBLISHED, result.getVariants().getFirst().getState());
    }

    @Test
    void handle_throwsWhenPublishedToDrafted() {
        var productId = new ProductId("abc");
        var product = productWithCompleteVariant(productId, VariantState.PUBLISHED);
        when(productProvider.read(productId)).thenReturn(Optional.of(product));

        assertThrows(RuntimeException.class, () ->
                new UpdateStateOfVariant(productProvider).handle(
                        new UpdateStateOfVariant.Command(productId, product.getVariants().getFirst().getId(), VariantState.DRAFTED)
                )
        );
    }

    @Test
    void handle_throwsWhenPublishingWithoutPrice() {
        var productId = new ProductId("abc");
        var product = productWithCompleteVariant(productId, VariantState.DRAFTED);
        product.getVariants().getFirst().setPrice(null);
        when(productProvider.read(productId)).thenReturn(Optional.of(product));

        assertThrows(RuntimeException.class, () ->
                new UpdateStateOfVariant(productProvider).handle(
                        new UpdateStateOfVariant.Command(productId, product.getVariants().getFirst().getId(), VariantState.PUBLISHED)
                )
        );
    }

    @Test
    void handle_throwsWhenPublishingWithoutImages() {
        var productId = new ProductId("abc");
        var product = productWithCompleteVariant(productId, VariantState.DRAFTED);
        product.getVariants().getFirst().setImageIds(List.of());
        when(productProvider.read(productId)).thenReturn(Optional.of(product));

        assertThrows(RuntimeException.class, () ->
                new UpdateStateOfVariant(productProvider).handle(
                        new UpdateStateOfVariant.Command(productId, product.getVariants().getFirst().getId(), VariantState.PUBLISHED)
                )
        );
    }

    @Test
    void handle_throwsWhenVariantNotFound() {
        var productId = new ProductId("abc");
        var product = productWithCompleteVariant(productId, VariantState.DRAFTED);
        when(productProvider.read(productId)).thenReturn(Optional.of(product));

        assertThrows(RuntimeException.class, () ->
                new UpdateStateOfVariant(productProvider).handle(
                        new UpdateStateOfVariant.Command(productId, new VariantId("unknown"), VariantState.PUBLISHED)
                )
        );
    }

    private Product productWithCompleteVariant(ProductId productId, VariantState state) {
        var product = new Product();
        product.setId(productId);
        var variant = new Variant();
        variant.setId(new VariantId("v1"));
        variant.setState(state);
        variant.setPrice(new BigDecimal("29.90"));
        variant.setImageIds(List.of(new ImageId("img1")));
        product.setVariants(List.of(variant));
        return product;
    }

}
