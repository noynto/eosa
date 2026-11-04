package me.noynto.eosa.application;

import me.noynto.eosa.jewel.Jewel;
import me.noynto.eosa.jewel.JewelCategory;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.jewel.JewelState;
import me.noynto.eosa.shared.JewelId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadCategoryStatsTest {

    @Mock JewelProvider jewelProvider;

    @Test
    void handle_countsPublishedJewelsAndFindsTheLowestPricePerCategory() {
        publishedJewels(JewelCategory.NECKLACE, jewel("n1", "89.00"), jewel("n2", "49.50"));
        publishedJewels(JewelCategory.BRACELET, jewel("b1", "35.00"));

        var result = new ReadCategoryStats(jewelProvider).handle(
                new ReadCategoryStats.Query(List.of(JewelCategory.NECKLACE, JewelCategory.BRACELET)));

        assertEquals(List.of(
                new ReadCategoryStats.Stats(JewelCategory.NECKLACE, 2, new BigDecimal("49.50")),
                new ReadCategoryStats.Stats(JewelCategory.BRACELET, 1, new BigDecimal("35.00"))
        ), result.stats());
    }

    @Test
    void handle_hasNoPriceForACategoryWithoutPublishedJewel() {
        publishedJewels(JewelCategory.NECKLACE);

        var result = new ReadCategoryStats(jewelProvider).handle(
                new ReadCategoryStats.Query(List.of(JewelCategory.NECKLACE)));

        assertEquals(0, result.stats().getFirst().count());
        assertNull(result.stats().getFirst().minPrice());
    }

    private void publishedJewels(JewelCategory category, Jewel... jewels) {
        when(jewelProvider.readIds(Set.of(JewelState.PUBLISHED), Set.of(category)))
                .thenReturn(Stream.of(jewels).map(Jewel::getId));
        for (Jewel jewel : jewels) {
            when(jewelProvider.read(jewel.getId())).thenReturn(Optional.of(jewel));
        }
    }

    private static Jewel jewel(String id, String price) {
        var jewel = new Jewel();
        jewel.setId(new JewelId(id));
        jewel.setPrice(new BigDecimal(price));
        return jewel;
    }

}
