package me.noynto.eosa.application;

import me.noynto.eosa.charm.Charm;
import me.noynto.eosa.charm.CharmProvider;
import me.noynto.eosa.image.Image;
import me.noynto.eosa.image.ImageProvider;
import me.noynto.eosa.shared.CharmId;
import me.noynto.eosa.shared.ImageId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddImageToCharmTest {

    @Mock CharmProvider charmProvider;
    @Mock ImageProvider imageProvider;

    @Test
    void handle_uploadsImageAndUpdatesCharm() {
        var charmId = new CharmId("charm1");
        var charm = new Charm();
        charm.setId(charmId);
        when(charmProvider.read(charmId)).thenReturn(Optional.of(charm));
        when(charmProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var image = imageWith("photo");
        when(imageProvider.upload(any())).thenReturn(uploadedWith("img1"));

        new AddImageToCharm(charmProvider, imageProvider).handle(
                new AddImageToCharm.Command(charmId, image)
        );

        verify(charmProvider).write(argThat(c ->
                c.getImageId() != null && "img1".equals(c.getImageId().value())
        ));
    }

    @Test
    void handle_throwsWhenCharmNotFound() {
        var charmId = new CharmId("unknown");
        when(charmProvider.read(charmId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new AddImageToCharm(charmProvider, imageProvider).handle(
                        new AddImageToCharm.Command(charmId, imageWith("x"))
                )
        );
    }

    private Image imageWith(String name) {
        var image = new Image();
        image.setName(name);
        return image;
    }

    private Image uploadedWith(String id) {
        var image = new Image();
        image.setId(new ImageId(id));
        return image;
    }

}
