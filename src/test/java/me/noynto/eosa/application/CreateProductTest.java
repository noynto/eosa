package me.noynto.eosa.application;

import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.product.ProductState;
import me.noynto.eosa.shared.ProductId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateProductTest {

    @Mock ProductProvider productProvider;

    @Test
    void handle_writesProductWithName() {
        var expected = productWith("abc");
        when(productProvider.write(argThat(p -> "Lune".equals(p.getName()))))
                .thenReturn(expected);

        var result = new CreateProduct(productProvider).handle(
                new CreateProduct.Command("Lune")
        );

        assertEquals(expected, result);
    }

    @Test
    void handle_setsStateToDrafted() {
        when(productProvider.write(argThat(p -> true))).thenReturn(productWith("abc"));

        new CreateProduct(productProvider).handle(new CreateProduct.Command("Lune"));

        verify(productProvider).write(argThat(p ->
                "Lune".equals(p.getName()) &&
                ProductState.DRAFTED == p.getState()
        ));
    }

    private Product productWith(String id) {
        var product = new Product();
        product.setId(new ProductId(id));
        return product;
    }

}