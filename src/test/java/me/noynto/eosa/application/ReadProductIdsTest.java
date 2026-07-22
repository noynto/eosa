package me.noynto.eosa.application;

import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.product.ProductState;
import me.noynto.eosa.shared.OptionId;
import me.noynto.eosa.shared.OptionValueId;
import me.noynto.eosa.shared.ProductId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadProductIdsTest {

    @Mock ProductProvider productProvider;

    @Test
    void handle_returnsIdsMatchingStates() {
        var a = new ProductId("a");
        var b = new ProductId("b");
        when(productProvider.readIds(new ProductProvider.Search(Set.of(ProductState.PUBLISHED))))
                .thenReturn(Stream.of(a, b));

        var result = new ReadProductIds(productProvider).handle(
                new ReadProductIds.Query(Set.of(ProductState.PUBLISHED))
        );

        assertEquals(List.of(a, b), result);
    }

    @Test
    void handle_filtersByOptionValue() {
        var a = new ProductId("a");
        var optionId = new OptionId("type");
        var optionValueId = new OptionValueId("necklace");
        when(productProvider.readIds(new ProductProvider.Search(Set.of(ProductState.PUBLISHED), optionId, optionValueId)))
                .thenReturn(Stream.of(a));

        var result = new ReadProductIds(productProvider).handle(
                new ReadProductIds.Query(Set.of(ProductState.PUBLISHED), optionId, optionValueId)
        );

        assertEquals(List.of(a), result);
    }

    @Test
    void handle_appliesLimit() {
        var a = new ProductId("a");
        var b = new ProductId("b");
        when(productProvider.readIds(new ProductProvider.Search(Set.of(ProductState.PUBLISHED))))
                .thenReturn(Stream.of(a, b));

        var result = new ReadProductIds(productProvider).handle(
                new ReadProductIds.Query(Set.of(ProductState.PUBLISHED), null, null, 1)
        );

        assertEquals(List.of(a), result);
    }

}
