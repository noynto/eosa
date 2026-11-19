package me.noynto.eosa.application;

import me.noynto.eosa.collection.Collection;
import me.noynto.eosa.collection.CollectionProvider;
import me.noynto.eosa.collection.CollectionSeason;
import me.noynto.eosa.collection.CollectionState;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateCollectionTest {

    final CollectionId collectionId = new CollectionId("c1");

    @Mock CollectionProvider collectionProvider;

    @Test
    void handle_updatesTheFieldsAndKeepsTheStateAndTheJewels() {
        var collection = collection(collectionId, CollectionSeason.SUMMER, 2027);
        collection.setState(CollectionState.PUBLISHED);
        collection.setJewelIds(List.of(new JewelId("j1")));
        when(collectionProvider.read(collectionId)).thenReturn(Optional.of(collection));
        when(collectionProvider.readIds(null)).thenReturn(Stream.of(collectionId));
        when(collectionProvider.write(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = update(" Été 2027, suite ", CollectionSeason.SUMMER, 2027).collection();

        assertEquals("Été 2027, suite", result.getName());
        assertEquals(CollectionState.PUBLISHED, result.getState());
        assertEquals(List.of(new JewelId("j1")), result.getJewelIds());
    }

    @Test
    void handle_throwsWhenAnotherCollectionHasTheSeasonAndYear() {
        var other = new CollectionId("c2");
        when(collectionProvider.read(collectionId)).thenReturn(Optional.of(collection(collectionId, CollectionSeason.WINTER, 2027)));
        when(collectionProvider.readIds(null)).thenReturn(Stream.of(collectionId, other));
        when(collectionProvider.read(other)).thenReturn(Optional.of(collection(other, CollectionSeason.SUMMER, 2027)));

        assertThrows(UpdateCollection.InvalidCommand.class, () -> update("Été 2027", CollectionSeason.SUMMER, 2027));
        verify(collectionProvider, never()).write(any());
    }

    @Test
    void handle_throwsWhenTheCollectionDoesNotExist() {
        when(collectionProvider.read(collectionId)).thenReturn(Optional.empty());

        assertThrows(UpdateCollection.InvalidCommand.class, () -> update("Été 2027", CollectionSeason.SUMMER, 2027));
    }

    @Test
    void handle_throwsWhenTheNameSeasonOrYearIsInvalid() {
        assertThrows(UpdateCollection.InvalidCommand.class, () -> update("", CollectionSeason.SUMMER, 2027));
        assertThrows(UpdateCollection.InvalidCommand.class, () -> update("Été", null, 2027));
        assertThrows(UpdateCollection.InvalidCommand.class, () -> update("Été", CollectionSeason.SUMMER, 1999));
    }

    @Test
    void handle_savesTheTrimmedHeadlineAndBlurbAndTurnsBlankIntoNull() {
        when(collectionProvider.read(collectionId)).thenReturn(Optional.of(collection(collectionId, CollectionSeason.SUMMER, 2027)));
        when(collectionProvider.readIds(null)).thenReturn(Stream.of(collectionId));
        when(collectionProvider.write(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var written = update("Été 2027", CollectionSeason.SUMMER, 2027, " Lumière d'été ", "  ").collection();

        assertEquals("Lumière d'été", written.getHeadline());
        assertNull(written.getBlurb());
    }

    @Test
    void handle_throwsWhenTheHeadlineOrTheBlurbIsTooLong() {
        var headline = "x".repeat(Collection.MAX_HEADLINE_LENGTH + 1);
        var blurb = "x".repeat(Collection.MAX_BLURB_LENGTH + 1);

        assertThrows(UpdateCollection.InvalidCommand.class, () -> update("Été", CollectionSeason.SUMMER, 2027, headline, null));
        assertThrows(UpdateCollection.InvalidCommand.class, () -> update("Été", CollectionSeason.SUMMER, 2027, null, blurb));
        verify(collectionProvider, never()).write(any());
    }

    private UpdateCollection.Result update(String name, CollectionSeason season, int year) {
        return update(name, season, year, null, null);
    }

    private UpdateCollection.Result update(String name, CollectionSeason season, int year, String headline, String blurb) {
        return new UpdateCollection(collectionProvider).handle(new UpdateCollection.Command(collectionId, name, season, year, headline, blurb));
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
