package me.noynto.eosa.application;

import me.noynto.eosa.collection.Collection;
import me.noynto.eosa.collection.CollectionProvider;
import me.noynto.eosa.collection.CollectionState;
import me.noynto.eosa.jewel.Jewel;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.shared.CollectionId;
import me.noynto.eosa.shared.JewelId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateJewelsOfCollectionTest {

    final CollectionId collectionId = new CollectionId("c1");

    @Mock CollectionProvider collectionProvider;
    @Mock JewelProvider jewelProvider;

    @Test
    void handle_replacesTheJewelsAndKeepsEachOnce() {
        var collection = collection();
        var j1 = new JewelId("j1");
        var j2 = new JewelId("j2");
        when(collectionProvider.read(collectionId)).thenReturn(Optional.of(collection));
        when(jewelProvider.read(j1)).thenReturn(Optional.of(new Jewel()));
        when(jewelProvider.read(j2)).thenReturn(Optional.of(new Jewel()));
        when(collectionProvider.write(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = handle(List.of(j1, j2, j1)).collection();

        assertEquals(List.of(j1, j2), result.getJewelIds());
        assertEquals(CollectionState.DRAFTED, result.getState());
    }

    @Test
    void handle_emptiesTheCollectionWhenNoJewelIsGiven() {
        var collection = collection();
        collection.setJewelIds(List.of(new JewelId("j1")));
        when(collectionProvider.read(collectionId)).thenReturn(Optional.of(collection));
        when(collectionProvider.write(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(List.of(), handle(null).collection().getJewelIds());
    }

    @Test
    void handle_throwsWhenAJewelDoesNotExist() {
        var unknown = new JewelId("unknown");
        when(collectionProvider.read(collectionId)).thenReturn(Optional.of(collection()));
        when(jewelProvider.read(unknown)).thenReturn(Optional.empty());

        assertThrows(UpdateJewelsOfCollection.InvalidCommand.class, () -> handle(List.of(unknown)));
        verify(collectionProvider, never()).write(any());
    }

    @Test
    void handle_throwsWhenTheCollectionDoesNotExist() {
        when(collectionProvider.read(collectionId)).thenReturn(Optional.empty());

        assertThrows(UpdateJewelsOfCollection.InvalidCommand.class, () -> handle(List.of()));
    }

    private UpdateJewelsOfCollection.Result handle(List<JewelId> jewelIds) {
        return new UpdateJewelsOfCollection(collectionProvider, jewelProvider)
                .handle(new UpdateJewelsOfCollection.Command(collectionId, jewelIds));
    }

    private Collection collection() {
        var collection = new Collection();
        collection.setId(collectionId);
        collection.setState(CollectionState.DRAFTED);
        return collection;
    }

}
