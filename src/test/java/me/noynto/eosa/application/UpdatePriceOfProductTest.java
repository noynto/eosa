package me.noynto.eosa.application;

import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.shared.ProductId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdatePriceOfProductTest {

    @Mock ProductProvider productProvider;

    @Test
    void handle_updatesPriceAndReturnsProduct() {
        var productId = new ProductId("abc");
        when(productProvider.read(productId)).thenReturn(Optional.of(productWith(productId)));
        when(productProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = new UpdatePriceOfProduct(productProvider).handle(
                new UpdatePriceOfProduct.Command(productId, new BigDecimal("29.90"))
        );

        assertEquals(new BigDecimal("29.90"), result.getPrice());
    }

    @Test
    void handle_delegatesWriteWithUpdatedPrice() {
        var productId = new ProductId("abc");
        when(productProvider.read(productId)).thenReturn(Optional.of(productWith(productId)));
        when(productProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        new UpdatePriceOfProduct(productProvider).handle(
                new UpdatePriceOfProduct.Command(productId, new BigDecimal("49.99"))
        );

        verify(productProvider).write(argThat(p -> new BigDecimal("49.99").equals(p.getPrice())));
    }

    @Test
    void handle_throwsWhenPriceIsNull() {
        assertThrows(RuntimeException.class, () ->
                new UpdatePriceOfProduct(productProvider).handle(
                        new UpdatePriceOfProduct.Command(new ProductId("abc"), null)
                )
        );
    }

    @Test
    void handle_throwsWhenPriceIsZero() {
        assertThrows(RuntimeException.class, () ->
                new UpdatePriceOfProduct(productProvider).handle(
                        new UpdatePriceOfProduct.Command(new ProductId("abc"), BigDecimal.ZERO)
                )
        );
    }

    @Test
    void handle_throwsWhenPriceIsNegative() {
        assertThrows(RuntimeException.class, () ->
                new UpdatePriceOfProduct(productProvider).handle(
                        new UpdatePriceOfProduct.Command(new ProductId("abc"), new BigDecimal("-5"))
                )
        );
    }

    @Test
    void handle_throwsWhenProductIdIsNull() {
        assertThrows(RuntimeException.class, () ->
                new UpdatePriceOfProduct(productProvider).handle(
                        new UpdatePriceOfProduct.Command(null, new BigDecimal("10"))
                )
        );
    }

    @Test
    void handle_throwsWhenProductNotFound() {
        var productId = new ProductId("unknown");
        when(productProvider.read(productId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new UpdatePriceOfProduct(productProvider).handle(
                        new UpdatePriceOfProduct.Command(productId, new BigDecimal("10"))
                )
        );
    }

    private Product productWith(ProductId id) {
        var product = new Product();
        product.setId(id);
        return product;
    }

}