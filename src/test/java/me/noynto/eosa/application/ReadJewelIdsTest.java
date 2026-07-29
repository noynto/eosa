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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadJewelIdsTest {

    @Mock JewelProvider jewelProvider;

    @Test
    void handle_returnsIdsAsList() {
        var states = Set.of(JewelState.PUBLISHED);
        var ids = List.of(new JewelId("a"), new JewelId("b"), new JewelId("c"));
        when(jewelProvider.readIds(eq(states), any())).thenReturn(ids.stream());

        var result = new ReadJewelIds(jewelProvider).handle(new ReadJewelIds.Query(states, Set.of()));

        assertEquals(ids, result);
    }

    @Test
    void handle_returnsEmptyListWhenNoJewels() {
        var states = Set.of(JewelState.PUBLISHED);
        when(jewelProvider.readIds(eq(states), any())).thenReturn(Stream.empty());

        var result = new ReadJewelIds(jewelProvider).handle(new ReadJewelIds.Query(states, Set.of()));

        assertEquals(List.of(), result);
    }

}