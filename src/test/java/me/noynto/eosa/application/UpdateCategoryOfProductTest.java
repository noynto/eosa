package me.noynto.eosa.application;

import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductCategory;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.shared.ProductId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateCategoryOfProductTest {

    @Mock ProductProvider productProvider;

    @Test
    void handle_updatesCategoryAndReturnsProduct() {
        var productId = new ProductId("abc");
        when(productProvider.read(productId)).thenReturn(Optional.of(productWith(productId)));
        when(productProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = new UpdateCategoryOfProduct(productProvider).handle(
                new UpdateCategoryOfProduct.Command(productId, ProductCategory.NECKLACE)
        );

        assertEquals(ProductCategory.NECKLACE, result.getCategory());
    }

    @Test
    void handle_delegatesWriteWithUpdatedCategory() {
        var productId = new ProductId("abc");
        when(productProvider.read(productId)).thenReturn(Optional.of(productWith(productId)));
        when(productProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        new UpdateCategoryOfProduct(productProvider).handle(
                new UpdateCategoryOfProduct.Command(productId, ProductCategory.BRACELET)
        );

        verify(productProvider).write(argThat(p -> ProductCategory.BRACELET == p.getCategory()));
    }

    @Test
    void handle_throwsWhenCategoryIsNull() {
        assertThrows(RuntimeException.class, () ->
                new UpdateCategoryOfProduct(productProvider).handle(
                        new UpdateCategoryOfProduct.Command(new ProductId("abc"), null)
                )
        );
    }

    @Test
    void handle_throwsWhenProductIdIsNull() {
        assertThrows(RuntimeException.class, () ->
                new UpdateCategoryOfProduct(productProvider).handle(
                        new UpdateCategoryOfProduct.Command(null, ProductCategory.NECKLACE)
                )
        );
    }

    @Test
    void handle_throwsWhenProductNotFound() {
        var productId = new ProductId("unknown");
        when(productProvider.read(productId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new UpdateCategoryOfProduct(productProvider).handle(
                        new UpdateCategoryOfProduct.Command(productId, ProductCategory.NECKLACE)
                )
        );
    }

    private Product productWith(ProductId id) {
        var product = new Product();
        product.setId(id);
        return product;
    }

}