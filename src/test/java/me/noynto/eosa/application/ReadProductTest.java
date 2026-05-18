package me.noynto.eosa.application;

import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.shared.ProductId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadProductTest {

    @Mock ProductProvider productProvider;

    @Test
    void handle_returnsProductFromProvider() throws Exception {
        var productId = new ProductId("abc");
        var expected = new Product();
        expected.setId(productId);
        when(productProvider.read(productId)).thenReturn(expected);

        var result = new ReadProduct(productProvider).handle(new ReadProduct.Command(productId));

        assertEquals(expected, result);
    }

    @Test
    void handle_propagatesUnknownProduct() throws Exception {
        var productId = new ProductId("unknown");
        when(productProvider.read(productId)).thenThrow(new ProductProvider.UnknownProduct(productId));

        assertThrows(ProductProvider.UnknownProduct.class, () ->
                new ReadProduct(productProvider).handle(new ReadProduct.Command(productId))
        );
    }

}