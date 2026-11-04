package me.noynto.eosa.application;

import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.jewel.JewelState;
import me.noynto.eosa.shared.JewelId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadRelatedJewelsTest {

    @Mock JewelProvider jewelProvider;

    private final JewelId current = new JewelId("current");

    @Test
    void handle_proposesPublishedJewelsWithoutTheCurrentOne() {
        publishedJewels("current", "a", "b", "c", "d", "e");

        var result = new ReadRelatedJewels(jewelProvider).handle(new ReadRelatedJewels.Query(current, 4));

        assertEquals(4, result.jewelIds().size());
        assertFalse(result.jewelIds().contains(current));
        assertTrue(Set.of(new JewelId("a"), new JewelId("b"), new JewelId("c"), new JewelId("d"), new JewelId("e"))
                .containsAll(result.jewelIds()));
    }

    @Test
    void handle_returnsFewerWhenThereAreNotEnoughJewels() {
        publishedJewels("current", "a");

        var result = new ReadRelatedJewels(jewelProvider).handle(new ReadRelatedJewels.Query(current, 4));

        assertEquals(List.of(new JewelId("a")), result.jewelIds());
    }

    @Test
    void handle_returnsNothingWhenTheCurrentJewelIsTheOnlyOne() {
        publishedJewels("current");

        assertTrue(new ReadRelatedJewels(jewelProvider).handle(new ReadRelatedJewels.Query(current, 4)).jewelIds().isEmpty());
    }

    @Test
    void handle_requiresAJewelAndAPositiveLimit() {
        assertThrows(RuntimeException.class, () -> new ReadRelatedJewels(jewelProvider).handle(new ReadRelatedJewels.Query(null, 4)));
        assertThrows(RuntimeException.class, () -> new ReadRelatedJewels(jewelProvider).handle(new ReadRelatedJewels.Query(current, 0)));
    }

    private void publishedJewels(String... ids) {
        when(jewelProvider.readIds(Set.of(JewelState.PUBLISHED), Set.of()))
                .thenReturn(Stream.of(ids).map(JewelId::new));
    }

}
