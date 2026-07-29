package me.noynto.eosa.application;

import me.noynto.eosa.image.Image;
import me.noynto.eosa.image.ImageProvider;
import me.noynto.eosa.jewel.Jewel;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.JewelId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddImagesToJewelTest {

    @Mock JewelProvider jewelProvider;
    @Mock ImageProvider imageProvider;

    @Test
    void handle_uploadsEachImageAndUpdatesJewel() {
        var jewelId = new JewelId("prod1");
        var jewel = new Jewel();
        jewel.setId(jewelId);
        when(jewelProvider.read(jewelId)).thenReturn(Optional.of(jewel));
        when(jewelProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var img1 = imageWith("img1");
        var img2 = imageWith("img2");
        when(imageProvider.upload(any()))
                .thenAnswer(inv -> uploadedWith("id-" + ((Image) inv.getArgument(0)).getName()));

        new AddImagesToJewel(jewelProvider, imageProvider).handle(
                new AddImagesToJewel.Command(jewelId, List.of(img1, img2))
        );

        verify(jewelProvider).write(argThat(p ->
                p.getImageIds().size() == 2 &&
                p.getImageIds().getFirst().value().equals("id-img1") &&
                p.getImageIds().get(1).value().equals("id-img2")
        ));
    }

    @Test
    void handle_preservesExistingImageIds() {
        var jewelId = new JewelId("prod1");
        var jewel = new Jewel();
        jewel.setId(jewelId);
        jewel.setImageIds(List.of(new ImageId("existing")));
        when(jewelProvider.read(jewelId)).thenReturn(Optional.of(jewel));
        when(jewelProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));
        when(imageProvider.upload(any())).thenReturn(uploadedWith("new"));

        new AddImagesToJewel(jewelProvider, imageProvider).handle(
                new AddImagesToJewel.Command(jewelId, List.of(imageWith("new")))
        );

        verify(jewelProvider).write(argThat(p ->
                p.getImageIds().size() == 2 &&
                p.getImageIds().getFirst().value().equals("existing") &&
                p.getImageIds().get(1).value().equals("new")
        ));
    }

    @Test
    void handle_throwsWhenJewelNotFound() {
        var jewelId = new JewelId("unknown");
        when(jewelProvider.read(jewelId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new AddImagesToJewel(jewelProvider, imageProvider).handle(
                        new AddImagesToJewel.Command(jewelId, List.of(imageWith("x")))
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