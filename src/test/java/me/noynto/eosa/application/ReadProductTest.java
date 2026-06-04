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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadProductTest {

    @Mock ProductProvider productProvider;

    @Test
    void handle_returnsProductFromProvider() {
        var productId = new ProductId("abc");
        var expected = new Product();
        expected.setId(productId);
        when(productProvider.read(productId)).thenReturn(Optional.of(expected));

        var result = new ReadProduct(productProvider).handle(new ReadProduct.Command(productId));

        assertEquals(expected, result);
    }

    @Test
    void handle_throwsWhenProductNotFound() {
        var productId = new ProductId("unknown");
        when(productProvider.read(productId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new ReadProduct(productProvider).handle(new ReadProduct.Command(productId))
        );
    }

}