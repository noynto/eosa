package me.noynto.eosa.application;

import me.noynto.eosa.charm.CharmProvider;
import me.noynto.eosa.charm.CharmState;
import me.noynto.eosa.shared.CharmId;
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
class ReadCharmIdsTest {

    @Mock CharmProvider charmProvider;

    @Test
    void handle_returnsMatchingIds() {
        var a = new CharmId("a");
        var b = new CharmId("b");
        when(charmProvider.readIds(Set.of(CharmState.PUBLISHED))).thenReturn(Stream.of(a, b));

        var result = new ReadCharmIds(charmProvider).handle(new ReadCharmIds.Query(Set.of(CharmState.PUBLISHED)));

        assertEquals(List.of(a, b), result);
    }

    @Test
    void handle_appliesLimit() {
        var a = new CharmId("a");
        var b = new CharmId("b");
        when(charmProvider.readIds(Set.of(CharmState.PUBLISHED))).thenReturn(Stream.of(a, b));

        var result = new ReadCharmIds(charmProvider).handle(new ReadCharmIds.Query(Set.of(CharmState.PUBLISHED), 1));

        assertEquals(List.of(a), result);
    }

}
