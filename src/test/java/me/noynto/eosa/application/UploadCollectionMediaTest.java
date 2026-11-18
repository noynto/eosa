package me.noynto.eosa.application;

import me.noynto.eosa.collection.Collection;
import me.noynto.eosa.collection.CollectionProvider;
import me.noynto.eosa.image.Image;
import me.noynto.eosa.image.ImageProvider;
import me.noynto.eosa.shared.CollectionId;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.VideoId;
import me.noynto.eosa.video.Video;
import me.noynto.eosa.video.VideoProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadCollectionMediaTest {

    final CollectionId collectionId = new CollectionId("c1");

    @Mock CollectionProvider collectionProvider;
    @Mock VideoProvider videoProvider;
    @Mock ImageProvider imageProvider;

    @Test
    void handle_storesTheVideoAndThePosterOnTheCollection() {
        when(collectionProvider.read(collectionId)).thenReturn(Optional.of(collection()));
        var video = video("video/mp4", 100);
        var poster = poster("image/webp");
        when(videoProvider.write(video)).thenAnswer(invocation -> withId(video, "v1"));
        when(imageProvider.upload(poster)).thenAnswer(invocation -> withId(poster, "i1"));
        when(collectionProvider.write(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = handle(video, poster).collection();

        assertEquals(new VideoId("v1"), result.getVideoId());
        assertEquals(new ImageId("i1"), result.getPosterImageId());
        verify(videoProvider, never()).delete(any());
        verify(imageProvider, never()).delete(any());
    }

    @Test
    void handle_deletesThePreviousMediaAfterSavingTheCollection() {
        var collection = collection();
        collection.setVideoId(new VideoId("old-video"));
        collection.setPosterImageId(new ImageId("old-poster"));
        when(collectionProvider.read(collectionId)).thenReturn(Optional.of(collection));
        var video = video("video/mp4", 100);
        var poster = poster("image/png");
        when(videoProvider.write(video)).thenAnswer(invocation -> withId(video, "v2"));
        when(imageProvider.upload(poster)).thenAnswer(invocation -> withId(poster, "i2"));
        when(collectionProvider.write(any())).thenAnswer(invocation -> invocation.getArgument(0));

        handle(video, poster);

        verify(videoProvider).delete(new VideoId("old-video"));
        verify(imageProvider).delete(new ImageId("old-poster"));
    }

    @Test
    void handle_keepsThePosterWhenOnlyTheVideoIsSent() {
        var collection = collection();
        collection.setPosterImageId(new ImageId("poster"));
        when(collectionProvider.read(collectionId)).thenReturn(Optional.of(collection));
        var video = video("video/mp4", 100);
        when(videoProvider.write(video)).thenAnswer(invocation -> withId(video, "v1"));
        when(collectionProvider.write(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = handle(video, null).collection();

        assertEquals(new ImageId("poster"), result.getPosterImageId());
        verify(imageProvider, never()).upload(any());
        verify(imageProvider, never()).delete(any());
    }

    @Test
    void handle_refusesAVideoThatIsNotAnMp4() {
        assertThrows(UploadCollectionMedia.InvalidCommand.class, () -> handle(video("video/webm", 100), null));
        verify(videoProvider, never()).write(any());
    }

    @Test
    void handle_refusesAVideoOverTheLimit() {
        assertThrows(UploadCollectionMedia.InvalidCommand.class, () -> handle(video("video/mp4", Video.MAX_SIZE + 1), null));
        verify(videoProvider, never()).write(any());
    }

    @Test
    void handle_refusesAnEmptyVideo() {
        assertThrows(UploadCollectionMedia.InvalidCommand.class, () -> handle(video("video/mp4", 0), null));
    }

    @Test
    void handle_refusesAPosterWhoseFormatIsNotAnImage() {
        assertThrows(UploadCollectionMedia.InvalidCommand.class, () -> handle(null, poster("image/svg+xml")));
        verify(imageProvider, never()).upload(any());
    }

    @Test
    void handle_refusesWhenNothingIsSent() {
        assertThrows(UploadCollectionMedia.InvalidCommand.class, () -> handle(null, null));
    }

    @Test
    void handle_throwsWhenTheCollectionDoesNotExist() {
        when(collectionProvider.read(collectionId)).thenReturn(Optional.empty());

        assertThrows(UploadCollectionMedia.InvalidCommand.class, () -> handle(video("video/mp4", 100), null));
        verify(videoProvider, never()).write(any());
    }

    private UploadCollectionMedia.Result handle(Video video, Image poster) {
        return new UploadCollectionMedia(collectionProvider, videoProvider, imageProvider)
                .handle(new UploadCollectionMedia.Command(collectionId, video, poster));
    }

    private Collection collection() {
        var collection = new Collection();
        collection.setId(collectionId);
        return collection;
    }

    private static Video video(String format, int size) {
        var video = new Video();
        video.setFormat(format);
        video.setContent(new byte[size]);
        return video;
    }

    private static Image poster(String format) {
        var image = new Image();
        image.setFormat(format);
        return image;
    }

    private static Video withId(Video video, String id) {
        video.setId(new VideoId(id));
        return video;
    }

    private static Image withId(Image image, String id) {
        image.setId(new ImageId(id));
        return image;
    }

}
