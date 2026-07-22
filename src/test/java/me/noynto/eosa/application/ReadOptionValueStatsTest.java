package me.noynto.eosa.application;

import me.noynto.eosa.product.Product;
import me.noynto.eosa.product.ProductProvider;
import me.noynto.eosa.product.ProductState;
import me.noynto.eosa.product.Variant;
import me.noynto.eosa.shared.OptionId;
import me.noynto.eosa.shared.OptionValueId;
import me.noynto.eosa.shared.ProductId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadOptionValueStatsTest {

    @Mock ProductProvider productProvider;

    @Test
    void handle_countsAndFindsMinPrice() {
        var typeId = new OptionId("type");
        var necklaceId = new OptionValueId("necklace");
        var productId = new ProductId("abc");

        when(productProvider.readIds(new ProductProvider.Search(Set.of(ProductState.PUBLISHED), typeId, necklaceId)))
                .thenReturn(Stream.of(productId));

        var product = new Product();
        product.setId(productId);
        var matchingVariant = new Variant();
        matchingVariant.setOptionValues(Map.of(typeId, necklaceId));
        matchingVariant.setPrice(new BigDecimal("19.90"));
        var otherVariant = new Variant();
        otherVariant.setOptionValues(Map.of(typeId, new OptionValueId("bracelet")));
        otherVariant.setPrice(new BigDecimal("5.00"));
        product.setVariants(List.of(matchingVariant, otherVariant));
        when(productProvider.read(productId)).thenReturn(Optional.of(product));

        var readProductIds = new ReadProductIds(productProvider);
        var result = new ReadOptionValueStats(productProvider, readProductIds).handle(typeId, necklaceId);

        assertEquals(1, result.count());
        assertEquals(new BigDecimal("19.90"), result.minPrice());
    }

    @Test
    void handle_returnsNullMinPriceWhenNoMatch() {
        var typeId = new OptionId("type");
        var necklaceId = new OptionValueId("necklace");
        when(productProvider.readIds(new ProductProvider.Search(Set.of(ProductState.PUBLISHED), typeId, necklaceId)))
                .thenReturn(Stream.empty());

        var readProductIds = new ReadProductIds(productProvider);
        var result = new ReadOptionValueStats(productProvider, readProductIds).handle(typeId, necklaceId);

        assertEquals(0, result.count());
        assertEquals(null, result.minPrice());
    }

}
