package me.noynto.eosa.application;

import me.noynto.eosa.charm.Charm;
import me.noynto.eosa.charm.CharmProvider;
import me.noynto.eosa.charm.CharmState;
import me.noynto.eosa.shared.CharmId;
import me.noynto.eosa.shared.ImageId;
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
class UpdateStateOfCharmTest {

    @Mock CharmProvider charmProvider;

    @Test
    void handle_publishesCompleteCharm() {
        var charmId = new CharmId("abc");
        var charm = completeCharm(charmId, CharmState.DRAFTED);
        when(charmProvider.read(charmId)).thenReturn(Optional.of(charm));
        when(charmProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = new UpdateStateOfCharm(charmProvider).handle(
                new UpdateStateOfCharm.Command(charmId, CharmState.PUBLISHED)
        );

        assertEquals(CharmState.PUBLISHED, result.getState());
    }

    @Test
    void handle_throwsWhenPublishedToDrafted() {
        var charmId = new CharmId("abc");
        var charm = completeCharm(charmId, CharmState.PUBLISHED);
        when(charmProvider.read(charmId)).thenReturn(Optional.of(charm));

        assertThrows(RuntimeException.class, () ->
                new UpdateStateOfCharm(charmProvider).handle(
                        new UpdateStateOfCharm.Command(charmId, CharmState.DRAFTED)
                )
        );
    }

    @Test
    void handle_throwsWhenPublishingWithoutAdditionalPrice() {
        var charmId = new CharmId("abc");
        var charm = completeCharm(charmId, CharmState.DRAFTED);
        charm.setAdditionalPrice(null);
        when(charmProvider.read(charmId)).thenReturn(Optional.of(charm));

        assertThrows(RuntimeException.class, () ->
                new UpdateStateOfCharm(charmProvider).handle(
                        new UpdateStateOfCharm.Command(charmId, CharmState.PUBLISHED)
                )
        );
    }

    @Test
    void handle_throwsWhenPublishingWithoutImage() {
        var charmId = new CharmId("abc");
        var charm = completeCharm(charmId, CharmState.DRAFTED);
        charm.setImageId(null);
        when(charmProvider.read(charmId)).thenReturn(Optional.of(charm));

        assertThrows(RuntimeException.class, () ->
                new UpdateStateOfCharm(charmProvider).handle(
                        new UpdateStateOfCharm.Command(charmId, CharmState.PUBLISHED)
                )
        );
    }

    @Test
    void handle_throwsWhenCharmNotFound() {
        var charmId = new CharmId("unknown");
        when(charmProvider.read(charmId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new UpdateStateOfCharm(charmProvider).handle(
                        new UpdateStateOfCharm.Command(charmId, CharmState.PUBLISHED)
                )
        );
    }

    private Charm completeCharm(CharmId id, CharmState state) {
        var charm = new Charm();
        charm.setId(id);
        charm.setState(state);
        charm.setName("Lune");
        charm.setAdditionalPrice(new BigDecimal("5.00"));
        charm.setImageId(new ImageId("img1"));
        return charm;
    }

}
