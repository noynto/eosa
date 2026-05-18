package me.noynto.eosa.application;

import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.shared.ProductId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateProductTest {

    @Mock ProductProvider productProvider;

    @Test
    void handle_writesProductWithCommandData() {
        var expected = productWith("abc");
        when(productProvider.write(argThat(p ->
                "Lune".equals(p.getName()) &&
                "Collier fin".equals(p.getDescription()) &&
                new BigDecimal("42").equals(p.getPrice())
        ))).thenReturn(expected);

        var result = new CreateProduct(productProvider).handle(
                new CreateProduct.Command("Lune", "Collier fin", new BigDecimal("42"))
        );

        assertEquals(expected, result);
    }

    @Test
    void handle_delegatesWriteToProvider() {
        when(productProvider.write(argThat(p -> true))).thenReturn(productWith("abc"));

        new CreateProduct(productProvider).handle(
                new CreateProduct.Command("Lune", "Collier fin", new BigDecimal("42"))
        );

        verify(productProvider).write(argThat(p ->
                "Lune".equals(p.getName()) &&
                new BigDecimal("42").equals(p.getPrice())
        ));
    }

    private Product productWith(String id) {
        var product = new Product();
        product.setId(new ProductId(id));
        return product;
    }

}