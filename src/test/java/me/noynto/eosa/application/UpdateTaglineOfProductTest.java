package me.noynto.eosa.application;

import me.noynto.eosa.product.Product;
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
class UpdateTaglineOfProductTest {

    @Mock ProductProvider productProvider;

    @Test
    void handle_updatesTaglineAndReturnsProduct() {
        var productId = new ProductId("abc");
        var product = productWith(productId);
        when(productProvider.read(productId)).thenReturn(Optional.of(product));
        when(productProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = new UpdateTaglineOfProduct(productProvider).handle(
                new UpdateTaglineOfProduct.Command(productId, "Bijou élégant")
        );

        assertEquals("Bijou élégant", result.getTagline());
    }

    @Test
    void handle_delegatesWriteWithUpdatedTagline() {
        var productId = new ProductId("abc");
        when(productProvider.read(productId)).thenReturn(Optional.of(productWith(productId)));
        when(productProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        new UpdateTaglineOfProduct(productProvider).handle(
                new UpdateTaglineOfProduct.Command(productId, "Bijou élégant")
        );

        verify(productProvider).write(argThat(p -> "Bijou élégant".equals(p.getTagline())));
    }

    @Test
    void handle_throwsWhenTaglineIsNull() {
        assertThrows(RuntimeException.class, () ->
                new UpdateTaglineOfProduct(productProvider).handle(
                        new UpdateTaglineOfProduct.Command(new ProductId("abc"), null)
                )
        );
    }

    @Test
    void handle_throwsWhenTaglineIsBlank() {
        assertThrows(RuntimeException.class, () ->
                new UpdateTaglineOfProduct(productProvider).handle(
                        new UpdateTaglineOfProduct.Command(new ProductId("abc"), "  ")
                )
        );
    }

    @Test
    void handle_throwsWhenProductIdIsNull() {
        assertThrows(RuntimeException.class, () ->
                new UpdateTaglineOfProduct(productProvider).handle(
                        new UpdateTaglineOfProduct.Command(null, "Bijou élégant")
                )
        );
    }

    @Test
    void handle_throwsWhenProductNotFound() {
        var productId = new ProductId("unknown");
        when(productProvider.read(productId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new UpdateTaglineOfProduct(productProvider).handle(
                        new UpdateTaglineOfProduct.Command(productId, "Bijou élégant")
                )
        );
    }

    private Product productWith(ProductId id) {
        var product = new Product();
        product.setId(id);
        return product;
    }

}