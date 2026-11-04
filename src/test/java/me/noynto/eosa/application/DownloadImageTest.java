package me.noynto.eosa.application;

import me.noynto.eosa.image.Image;
import me.noynto.eosa.image.ImageProvider;
import me.noynto.eosa.shared.ImageId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DownloadImageTest {

    @Mock ImageProvider imageProvider;

    @Test
    void handle_returnsImageWhenFound() {
        var imageId = new ImageId("abc");
        var expected = new Image();
        expected.setId(imageId);
        when(imageProvider.download(imageId)).thenReturn(Optional.of(expected));

        var result = new DownloadImage(imageProvider).handle(new DownloadImage.Command(imageId));

        assertEquals(expected, result.image());
    }

    @Test
    void handle_throwsWhenTheImageDoesNotExist() {
        var imageId = new ImageId("unknown");
        when(imageProvider.download(imageId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> new DownloadImage(imageProvider).handle(new DownloadImage.Command(imageId)));
    }

}