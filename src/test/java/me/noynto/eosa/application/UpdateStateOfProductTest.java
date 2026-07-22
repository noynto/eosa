package me.noynto.eosa.application;

import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductState;
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
class UpdateStateOfProductTest {

    @Mock ProductProvider productProvider;

    @Test
    void handle_publishesCompleteProduct() {
        var productId = new ProductId("abc");
        var product = completeProduct(productId, ProductState.DRAFTED);
        when(productProvider.read(productId)).thenReturn(Optional.of(product));
        when(productProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = new UpdateStateOfProduct(productProvider).handle(
                new UpdateStateOfProduct.Command(productId, ProductState.PUBLISHED)
        );

        assertEquals(ProductState.PUBLISHED, result.getState());
    }

    @Test
    void handle_archivesPublishedProduct() {
        var productId = new ProductId("abc");
        var product = completeProduct(productId, ProductState.PUBLISHED);
        when(productProvider.read(productId)).thenReturn(Optional.of(product));
        when(productProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = new UpdateStateOfProduct(productProvider).handle(
                new UpdateStateOfProduct.Command(productId, ProductState.ARCHIVED)
        );

        assertEquals(ProductState.ARCHIVED, result.getState());
    }

    @Test
    void handle_throwsWhenStateIsNull() {
        assertThrows(RuntimeException.class, () ->
                new UpdateStateOfProduct(productProvider).handle(
                        new UpdateStateOfProduct.Command(new ProductId("abc"), null)
                )
        );
    }

    @Test
    void handle_throwsWhenProductIdIsNull() {
        assertThrows(RuntimeException.class, () ->
                new UpdateStateOfProduct(productProvider).handle(
                        new UpdateStateOfProduct.Command(null, ProductState.PUBLISHED)
                )
        );
    }

    @Test
    void handle_throwsWhenProductNotFound() {
        var productId = new ProductId("unknown");
        when(productProvider.read(productId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new UpdateStateOfProduct(productProvider).handle(
                        new UpdateStateOfProduct.Command(productId, ProductState.PUBLISHED)
                )
        );
    }

    @Test
    void handle_throwsWhenAlreadyInSameState() {
        var productId = new ProductId("abc");
        var product = completeProduct(productId, ProductState.DRAFTED);
        when(productProvider.read(productId)).thenReturn(Optional.of(product));

        assertThrows(RuntimeException.class, () ->
                new UpdateStateOfProduct(productProvider).handle(
                        new UpdateStateOfProduct.Command(productId, ProductState.DRAFTED)
                )
        );
    }

    @Test
    void handle_throwsWhenPublishedToDrafted() {
        var productId = new ProductId("abc");
        var product = completeProduct(productId, ProductState.PUBLISHED);
        when(productProvider.read(productId)).thenReturn(Optional.of(product));

        assertThrows(RuntimeException.class, () ->
                new UpdateStateOfProduct(productProvider).handle(
                        new UpdateStateOfProduct.Command(productId, ProductState.DRAFTED)
                )
        );
    }

    @Test
    void handle_throwsWhenPublishingWithoutName() {
        var productId = new ProductId("abc");
        var product = completeProduct(productId, ProductState.DRAFTED);
        product.setName(null);
        when(productProvider.read(productId)).thenReturn(Optional.of(product));

        assertThrows(RuntimeException.class, () ->
                new UpdateStateOfProduct(productProvider).handle(
                        new UpdateStateOfProduct.Command(productId, ProductState.PUBLISHED)
                )
        );
    }

    @Test
    void handle_throwsWhenPublishingWithoutDescription() {
        var productId = new ProductId("abc");
        var product = completeProduct(productId, ProductState.DRAFTED);
        product.setDescription(null);
        when(productProvider.read(productId)).thenReturn(Optional.of(product));

        assertThrows(RuntimeException.class, () ->
                new UpdateStateOfProduct(productProvider).handle(
                        new UpdateStateOfProduct.Command(productId, ProductState.PUBLISHED)
                )
        );
    }

    @Test
    void handle_throwsWhenPublishingWithoutVariants() {
        var productId = new ProductId("abc");
        var product = completeProduct(productId, ProductState.DRAFTED);
        product.setVariants(List.of());
        when(productProvider.read(productId)).thenReturn(Optional.of(product));

        assertThrows(RuntimeException.class, () ->
                new UpdateStateOfProduct(productProvider).handle(
                        new UpdateStateOfProduct.Command(productId, ProductState.PUBLISHED)
                )
        );
    }

    @Test
    void handle_throwsWhenPublishingWithoutDefaultVariant() {
        var productId = new ProductId("abc");
        var product = completeProduct(productId, ProductState.DRAFTED);
        product.setDefaultVariantId(null);
        when(productProvider.read(productId)).thenReturn(Optional.of(product));

        assertThrows(RuntimeException.class, () ->
                new UpdateStateOfProduct(productProvider).handle(
                        new UpdateStateOfProduct.Command(productId, ProductState.PUBLISHED)
                )
        );
    }

    private Product completeProduct(ProductId id, ProductState state) {
        var product = new Product();
        product.setId(id);
        product.setState(state);
        product.setName("Pauline");
        product.setDescription("Bijou élégant");
        var variant = new Variant();
        variant.setId(new VariantId("v1"));
        product.setVariants(List.of(variant));
        product.setDefaultVariantId(variant.getId());
        return product;
    }

}
