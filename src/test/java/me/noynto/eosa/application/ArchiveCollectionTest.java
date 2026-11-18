package me.noynto.eosa.application;

import me.noynto.eosa.collection.Collection;
import me.noynto.eosa.collection.CollectionProvider;
import me.noynto.eosa.collection.CollectionState;
import me.noynto.eosa.shared.CollectionId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArchiveCollectionTest {

    final CollectionId collectionId = new CollectionId("c1");

    @Mock CollectionProvider collectionProvider;

    @Test
    void handle_archivesAPublishedCollectionAndKeepsItsDate() {
        var published = Instant.parse("2026-11-01T10:00:00Z");
        var collection = collection(CollectionState.PUBLISHED);
        collection.setPublishedAt(published);
        when(collectionProvider.read(collectionId)).thenReturn(Optional.of(collection));
        when(collectionProvider.write(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = handle().collection();

        assertEquals(CollectionState.ARCHIVED, result.getState());
        assertEquals(published, result.getPublishedAt());
    }

    @Test
    void handle_refusesADraftAndAnArchivedCollection() {
        when(collectionProvider.read(collectionId)).thenReturn(Optional.of(collection(CollectionState.DRAFTED)));
        assertThrows(ArchiveCollection.InvalidCommand.class, this::handle);

        when(collectionProvider.read(collectionId)).thenReturn(Optional.of(collection(CollectionState.ARCHIVED)));
        assertThrows(ArchiveCollection.InvalidCommand.class, this::handle);

        verify(collectionProvider, never()).write(any());
    }

    @Test
    void handle_throwsWhenTheCollectionDoesNotExist() {
        when(collectionProvider.read(collectionId)).thenReturn(Optional.empty());

        assertThrows(ArchiveCollection.InvalidCommand.class, this::handle);
    }

    private ArchiveCollection.Result handle() {
        return new ArchiveCollection(collectionProvider).handle(new ArchiveCollection.Command(collectionId));
    }

    private Collection collection(CollectionState state) {
        var collection = new Collection();
        collection.setId(collectionId);
        collection.setState(state);
        return collection;
    }

}
