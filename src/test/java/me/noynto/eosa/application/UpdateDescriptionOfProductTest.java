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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateDescriptionOfProductTest {

    @Mock ProductProvider productProvider;

    @Test
    void handle_updatesDescription() {
        var productId = new ProductId("abc");
        var product = new Product();
        product.setId(productId);
        when(productProvider.read(productId)).thenReturn(Optional.of(product));
        when(productProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = new UpdateDescriptionOfProduct(productProvider).handle(
                new UpdateDescriptionOfProduct.Command(productId, "Un bijou intemporel")
        );

        assertEquals("Un bijou intemporel", result.getDescription());
    }

    @Test
    void handle_throwsWhenDescriptionBlank() {
        assertThrows(RuntimeException.class, () ->
                new UpdateDescriptionOfProduct(productProvider).handle(
                        new UpdateDescriptionOfProduct.Command(new ProductId("abc"), " ")
                )
        );
    }

    @Test
    void handle_throwsWhenProductNotFound() {
        var productId = new ProductId("unknown");
        when(productProvider.read(productId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new UpdateDescriptionOfProduct(productProvider).handle(
                        new UpdateDescriptionOfProduct.Command(productId, "Un bijou intemporel")
                )
        );
    }

}
