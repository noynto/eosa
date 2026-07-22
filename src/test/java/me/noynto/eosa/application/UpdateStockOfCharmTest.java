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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateStockOfCharmTest {

    @Mock CharmProvider charmProvider;

    @Test
    void handle_updatesStock() {
        var charmId = new CharmId("abc");
        var charm = new Charm();
        charm.setId(charmId);
        when(charmProvider.read(charmId)).thenReturn(Optional.of(charm));
        when(charmProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = new UpdateStockOfCharm(charmProvider).handle(
                new UpdateStockOfCharm.Command(charmId, 12)
        );

        assertEquals(12, result.getStock());
    }

    @Test
    void handle_throwsWhenNegative() {
        assertThrows(RuntimeException.class, () ->
                new UpdateStockOfCharm(charmProvider).handle(
                        new UpdateStockOfCharm.Command(new CharmId("abc"), -1)
                )
        );
    }

    @Test
    void handle_throwsWhenCharmNotFound() {
        var charmId = new CharmId("unknown");
        when(charmProvider.read(charmId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new UpdateStockOfCharm(charmProvider).handle(
                        new UpdateStockOfCharm.Command(charmId, 5)
                )
        );
    }

}
