package me.noynto.eosa.application;

import me.noynto.eosa.charm.Charm;
import me.noynto.eosa.charm.CharmProvider;
import me.noynto.eosa.shared.CharmId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadCharmTest {

    @Mock CharmProvider charmProvider;

    @Test
    void handle_returnsCharm() {
        var charmId = new CharmId("abc");
        var charm = new Charm();
        charm.setId(charmId);
        when(charmProvider.read(charmId)).thenReturn(Optional.of(charm));

        var result = new ReadCharm(charmProvider).handle(new ReadCharm.Command(charmId));

        assertEquals(charm, result);
    }

    @Test
    void handle_throwsWhenCharmNotFound() {
        var charmId = new CharmId("unknown");
        when(charmProvider.read(charmId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new ReadCharm(charmProvider).handle(new ReadCharm.Command(charmId))
        );
    }

}
