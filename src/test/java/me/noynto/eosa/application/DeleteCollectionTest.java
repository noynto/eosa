package me.noynto.eosa.application;

import me.noynto.eosa.collection.Collection;
import me.noynto.eosa.collection.CollectionProvider;
import me.noynto.eosa.collection.CollectionState;
import me.noynto.eosa.image.ImageProvider;
import me.noynto.eosa.shared.CollectionId;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.VideoId;
import me.noynto.eosa.video.VideoProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteCollectionTest {

    final CollectionId collectionId = new CollectionId("c1");

    @Mock CollectionProvider collectionProvider;
    @Mock VideoProvider videoProvider;
    @Mock ImageProvider imageProvider;

    @Test
    void handle_deletesTheCollectionItsVideoAndItsPoster() {
        var collection = collection(CollectionState.ARCHIVED);
        collection.setVideoId(new VideoId("v1"));
        collection.setPosterImageId(new ImageId("i1"));
        when(collectionProvider.read(collectionId)).thenReturn(Optional.of(collection));

        handle();

        verify(collectionProvider).delete(collectionId);
        verify(videoProvider).delete(new VideoId("v1"));
        verify(imageProvider).delete(new ImageId("i1"));
    }

    @Test
    void handle_deletesADraftWithoutMedia() {
        when(collectionProvider.read(collectionId)).thenReturn(Optional.of(collection(CollectionState.DRAFTED)));

        handle();

        verify(collectionProvider).delete(collectionId);
        verify(videoProvider, never()).delete(any());
        verify(imageProvider, never()).delete(any());
    }

    @Test
    void handle_refusesAPublishedCollection() {
        when(collectionProvider.read(collectionId)).thenReturn(Optional.of(collection(CollectionState.PUBLISHED)));

        assertThrows(DeleteCollection.InvalidCommand.class, this::handle);
        verify(collectionProvider, never()).delete(any());
    }

    @Test
    void handle_throwsWhenTheCollectionDoesNotExist() {
        when(collectionProvider.read(collectionId)).thenReturn(Optional.empty());

        assertThrows(DeleteCollection.InvalidCommand.class, this::handle);
    }

    private void handle() {
        new DeleteCollection(collectionProvider, videoProvider, imageProvider).handle(new DeleteCollection.Command(collectionId));
    }

    private Collection collection(CollectionState state) {
        var collection = new Collection();
        collection.setId(collectionId);
        collection.setState(state);
        return collection;
    }

}
