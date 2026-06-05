package me.noynto.eosa.application;

import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.product.ProductState;
import me.noynto.eosa.shared.ProductId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadProductIdsTest {

    @Mock ProductProvider productProvider;

    @Test
    void handle_returnsIdsAsList() {
        var states = Set.of(ProductState.PUBLISHED);
        var ids = List.of(new ProductId("a"), new ProductId("b"), new ProductId("c"));
        when(productProvider.readIds(eq(states), any())).thenReturn(ids.stream());

        var result = new ReadProductIds(productProvider).handle(new ReadProductIds.Query(states, Set.of()));

        assertEquals(ids, result);
    }

    @Test
    void handle_returnsEmptyListWhenNoProducts() {
        var states = Set.of(ProductState.PUBLISHED);
        when(productProvider.readIds(eq(states), any())).thenReturn(Stream.empty());

        var result = new ReadProductIds(productProvider).handle(new ReadProductIds.Query(states, Set.of()));

        assertEquals(List.of(), result);
    }

}