package me.noynto.eosa.application;

import me.noynto.eosa.collection.Collection;
import me.noynto.eosa.collection.CollectionProvider;
import me.noynto.eosa.collection.CollectionSeason;
import me.noynto.eosa.collection.CollectionState;
import me.noynto.eosa.shared.CollectionId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateCollectionTest {

    @Mock CollectionProvider collectionProvider;

    @Test
    void handle_writesADraftWithItsNameSeasonAndYear() {
        when(collectionProvider.readIds(null)).thenReturn(Stream.empty());
        when(collectionProvider.write(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = new CreateCollection(collectionProvider).handle(
                new CreateCollection.Command("  Été 2027 ", CollectionSeason.SUMMER, 2027)
        );

        assertEquals("Été 2027", result.collection().getName());
        verify(collectionProvider).write(argThat(c ->
                c.getState() == CollectionState.DRAFTED
                        && c.getSeason() == CollectionSeason.SUMMER
                        && c.getYear() == 2027
                        && c.getPublishedAt() == null
        ));
    }

    @Test
    void handle_throwsWhenTheNameIsBlank() {
        assertThrows(CreateCollection.InvalidCommand.class, () -> new CreateCollection(collectionProvider).handle(
                new CreateCollection.Command(" ", CollectionSeason.SUMMER, 2027)));
        verify(collectionProvider, never()).write(any());
    }

    @Test
    void handle_throwsWhenTheSeasonIsMissing() {
        assertThrows(CreateCollection.InvalidCommand.class, () -> new CreateCollection(collectionProvider).handle(
                new CreateCollection.Command("Été 2027", null, 2027)));
    }

    @Test
    void handle_throwsWhenTheYearIsOutOfBounds() {
        assertThrows(CreateCollection.InvalidCommand.class, () -> new CreateCollection(collectionProvider).handle(
                new CreateCollection.Command("Été 2023", CollectionSeason.SUMMER, Collection.MIN_YEAR - 1)));
        assertThrows(CreateCollection.InvalidCommand.class, () -> new CreateCollection(collectionProvider).handle(
                new CreateCollection.Command("Été 2101", CollectionSeason.SUMMER, Collection.MAX_YEAR + 1)));
    }

    @Test
    void handle_throwsWhenTheSeasonAndYearAreAlreadyUsed() {
        var existing = new Collection();
        existing.setSeason(CollectionSeason.SUMMER);
        existing.setYear(2027);
        var id = new CollectionId("c1");
        when(collectionProvider.readIds(null)).thenReturn(Stream.of(id));
        when(collectionProvider.read(id)).thenReturn(Optional.of(existing));

        assertThrows(CreateCollection.InvalidCommand.class, () -> new CreateCollection(collectionProvider).handle(
                new CreateCollection.Command("Été 2027 bis", CollectionSeason.SUMMER, 2027)));
        verify(collectionProvider, never()).write(any());
    }

    @Test
    void handle_acceptsTheSameSeasonInAnotherYear() {
        var existing = new Collection();
        existing.setSeason(CollectionSeason.SUMMER);
        existing.setYear(2026);
        var id = new CollectionId("c1");
        when(collectionProvider.readIds(null)).thenReturn(Stream.of(id));
        when(collectionProvider.read(id)).thenReturn(Optional.of(existing));
        when(collectionProvider.write(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = new CreateCollection(collectionProvider).handle(
                new CreateCollection.Command("Été 2027", CollectionSeason.SUMMER, 2027));

        assertEquals(2027, result.collection().getYear());
    }

}
