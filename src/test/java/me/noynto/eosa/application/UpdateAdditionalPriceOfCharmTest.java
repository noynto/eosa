package me.noynto.eosa.application;

import me.noynto.eosa.charm.Charm;
import me.noynto.eosa.charm.CharmProvider;
import me.noynto.eosa.shared.CharmId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateAdditionalPriceOfCharmTest {

    @Mock CharmProvider charmProvider;

    @Test
    void handle_updatesAdditionalPrice() {
        var charmId = new CharmId("abc");
        var charm = new Charm();
        charm.setId(charmId);
        when(charmProvider.read(charmId)).thenReturn(Optional.of(charm));
        when(charmProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = new UpdateAdditionalPriceOfCharm(charmProvider).handle(
                new UpdateAdditionalPriceOfCharm.Command(charmId, new BigDecimal("5.00"))
        );

        assertEquals(new BigDecimal("5.00"), result.getAdditionalPrice());
    }

    @Test
    void handle_allowsZero() {
        var charmId = new CharmId("abc");
        var charm = new Charm();
        charm.setId(charmId);
        when(charmProvider.read(charmId)).thenReturn(Optional.of(charm));
        when(charmProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = new UpdateAdditionalPriceOfCharm(charmProvider).handle(
                new UpdateAdditionalPriceOfCharm.Command(charmId, BigDecimal.ZERO)
        );

        assertEquals(BigDecimal.ZERO, result.getAdditionalPrice());
    }

    @Test
    void handle_throwsWhenNegative() {
        assertThrows(RuntimeException.class, () ->
                new UpdateAdditionalPriceOfCharm(charmProvider).handle(
                        new UpdateAdditionalPriceOfCharm.Command(new CharmId("abc"), new BigDecimal("-1"))
                )
        );
    }

    @Test
    void handle_throwsWhenNull() {
        assertThrows(RuntimeException.class, () ->
                new UpdateAdditionalPriceOfCharm(charmProvider).handle(
                        new UpdateAdditionalPriceOfCharm.Command(new CharmId("abc"), null)
                )
        );
    }

    @Test
    void handle_throwsWhenCharmNotFound() {
        var charmId = new CharmId("unknown");
        when(charmProvider.read(charmId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new UpdateAdditionalPriceOfCharm(charmProvider).handle(
                        new UpdateAdditionalPriceOfCharm.Command(charmId, new BigDecimal("5.00"))
                )
        );
    }

}
