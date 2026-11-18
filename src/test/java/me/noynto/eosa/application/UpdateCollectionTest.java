package me.noynto.eosa.application;

import me.noynto.eosa.collection.Collection;
import me.noynto.eosa.collection.CollectionProvider;
import me.noynto.eosa.collection.CollectionSeason;
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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateCollectionTest {

    final CollectionId collectionId = new CollectionId("c1");

    @Mock CollectionProvider collectionProvider;
    @Mock JewelProvider jewelProvider;

    @Test
    void handle_updatesTheFieldsAndKeepsTheStateAndTheMedia() {
        var collection = collection(collectionId, CollectionSeason.SUMMER, 2027);
        collection.setState(CollectionState.PUBLISHED);
        when(collectionProvider.read(collectionId)).thenReturn(Optional.of(collection));
        when(collectionProvider.readIds(null)).thenReturn(Stream.of(collectionId));
        when(collectionProvider.write(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = update(" Été 2027, suite ", CollectionSeason.SUMMER, 2027, List.of()).collection();

        assertEquals("Été 2027, suite", result.getName());
        assertEquals(CollectionState.PUBLISHED, result.getState());
    }

    @Test
    void handle_keepsEachJewelOnce() {
        var collection = collection(collectionId, CollectionSeason.SUMMER, 2027);
        var jewelId = new JewelId("j1");
        when(collectionProvider.read(collectionId)).thenReturn(Optional.of(collection));
        when(collectionProvider.readIds(null)).thenReturn(Stream.of(collectionId));
        when(jewelProvider.read(jewelId)).thenReturn(Optional.of(new Jewel()));
        when(collectionProvider.write(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = update("Été 2027", CollectionSeason.SUMMER, 2027, List.of(jewelId, jewelId)).collection();

        assertEquals(List.of(jewelId), result.getJewelIds());
    }

    @Test
    void handle_throwsWhenAJewelDoesNotExist() {
        var collection = collection(collectionId, CollectionSeason.SUMMER, 2027);
        var unknown = new JewelId("unknown");
        when(collectionProvider.read(collectionId)).thenReturn(Optional.of(collection));
        when(collectionProvider.readIds(null)).thenReturn(Stream.of(collectionId));
        when(jewelProvider.read(unknown)).thenReturn(Optional.empty());

        assertThrows(UpdateCollection.InvalidCommand.class, () -> update("Été 2027", CollectionSeason.SUMMER, 2027, List.of(unknown)));
        verify(collectionProvider, never()).write(any());
    }

    @Test
    void handle_throwsWhenAnotherCollectionHasTheSeasonAndYear() {
        var other = new CollectionId("c2");
        when(collectionProvider.read(collectionId)).thenReturn(Optional.of(collection(collectionId, CollectionSeason.WINTER, 2027)));
        when(collectionProvider.readIds(null)).thenReturn(Stream.of(collectionId, other));
        when(collectionProvider.read(other)).thenReturn(Optional.of(collection(other, CollectionSeason.SUMMER, 2027)));

        assertThrows(UpdateCollection.InvalidCommand.class, () -> update("Été 2027", CollectionSeason.SUMMER, 2027, List.of()));
        verify(collectionProvider, never()).write(any());
    }

    @Test
    void handle_throwsWhenTheCollectionDoesNotExist() {
        when(collectionProvider.read(collectionId)).thenReturn(Optional.empty());

        assertThrows(UpdateCollection.InvalidCommand.class, () -> update("Été 2027", CollectionSeason.SUMMER, 2027, List.of()));
    }

    @Test
    void handle_throwsWhenTheNameSeasonOrYearIsInvalid() {
        assertThrows(UpdateCollection.InvalidCommand.class, () -> update("", CollectionSeason.SUMMER, 2027, List.of()));
        assertThrows(UpdateCollection.InvalidCommand.class, () -> update("Été", null, 2027, List.of()));
        assertThrows(UpdateCollection.InvalidCommand.class, () -> update("Été", CollectionSeason.SUMMER, 1999, List.of()));
    }

    private UpdateCollection.Result update(String name, CollectionSeason season, int year, List<JewelId> jewelIds) {
        return new UpdateCollection(collectionProvider, jewelProvider).handle(
                new UpdateCollection.Command(collectionId, name, season, year, jewelIds));
    }

    private static Collection collection(CollectionId id, CollectionSeason season, int year) {
        var collection = new Collection();
        collection.setId(id);
        collection.setSeason(season);
        collection.setYear(year);
        collection.setState(CollectionState.DRAFTED);
        return collection;
    }

}
