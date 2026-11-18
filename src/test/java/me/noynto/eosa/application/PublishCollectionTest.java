package me.noynto.eosa.application;

import me.noynto.eosa.collection.Collection;
import me.noynto.eosa.collection.CollectionProvider;
import me.noynto.eosa.collection.CollectionState;
import me.noynto.eosa.jewel.Jewel;
import me.noynto.eosa.jewel.JewelCategory;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.jewel.JewelState;
import me.noynto.eosa.shared.CollectionId;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.JewelId;
import me.noynto.eosa.shared.VideoId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublishCollectionTest {

    final CollectionId collectionId = new CollectionId("c1");
    final Instant now = Instant.parse("2026-11-18T10:00:00Z");
    final Clock clock = Clock.fixed(now, ZoneOffset.UTC);

    @Mock CollectionProvider collectionProvider;
    @Mock JewelProvider jewelProvider;

    @Test
    void handle_publishesTheCollectionAndDatesIt() {
        var collection = collection(CollectionState.DRAFTED);
        when(collectionProvider.read(collectionId)).thenReturn(Optional.of(collection));
        when(collectionProvider.write(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = handle();

        assertEquals(CollectionState.PUBLISHED, result.collection().getState());
        assertEquals(now, result.collection().getPublishedAt());
        assertEquals(0, result.publishedJewelCount());
    }

    @Test
    void handle_publishesTheDraftedJewelsAndDatesThem() {
        var drafted = jewel("j1", JewelState.DRAFTED);
        var collection = collection(CollectionState.DRAFTED, drafted);
        when(collectionProvider.read(collectionId)).thenReturn(Optional.of(collection));
        when(jewelProvider.read(drafted.getId())).thenReturn(Optional.of(drafted));
        when(collectionProvider.write(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = handle();

        assertEquals(1, result.publishedJewelCount());
        verify(jewelProvider).write(drafted);
        assertEquals(JewelState.PUBLISHED, drafted.getState());
        assertEquals(now, drafted.getPublishedAt());
    }

    @Test
    void handle_leavesArchivedAndPublishedJewelsAlone() {
        var archived = jewel("j1", JewelState.ARCHIVED);
        var published = jewel("j2", JewelState.PUBLISHED);
        var earlier = Instant.parse("2026-01-05T10:00:00Z");
        published.setPublishedAt(earlier);
        var collection = collection(CollectionState.DRAFTED, archived, published);
        when(collectionProvider.read(collectionId)).thenReturn(Optional.of(collection));
        when(jewelProvider.read(archived.getId())).thenReturn(Optional.of(archived));
        when(jewelProvider.read(published.getId())).thenReturn(Optional.of(published));
        when(collectionProvider.write(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = handle();

        assertEquals(0, result.publishedJewelCount());
        assertEquals(JewelState.ARCHIVED, archived.getState());
        assertEquals(earlier, published.getPublishedAt());
        verify(jewelProvider, never()).write(any());
    }

    @Test
    void handle_keepsTheFirstPublicationDateOfAJewelAlreadyDated() {
        var drafted = jewel("j1", JewelState.DRAFTED);
        var earlier = Instant.parse("2026-01-05T10:00:00Z");
        drafted.setPublishedAt(earlier);
        var collection = collection(CollectionState.DRAFTED, drafted);
        when(collectionProvider.read(collectionId)).thenReturn(Optional.of(collection));
        when(jewelProvider.read(drafted.getId())).thenReturn(Optional.of(drafted));
        when(collectionProvider.write(any())).thenAnswer(invocation -> invocation.getArgument(0));

        handle();

        assertEquals(earlier, drafted.getPublishedAt());
    }

    @Test
    void handle_publishesAnArchivedCollectionAgainWithANewDate() {
        var collection = collection(CollectionState.ARCHIVED);
        collection.setPublishedAt(Instant.parse("2026-01-05T10:00:00Z"));
        when(collectionProvider.read(collectionId)).thenReturn(Optional.of(collection));
        when(collectionProvider.write(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = handle();

        assertEquals(CollectionState.PUBLISHED, result.collection().getState());
        assertEquals(now, result.collection().getPublishedAt());
    }

    @Test
    void handle_refusesAndWritesNothingWhenADraftedJewelIsIncomplete() {
        var complete = jewel("j1", JewelState.DRAFTED);
        var incomplete = jewel("j2", JewelState.DRAFTED);
        incomplete.setName("Lune");
        incomplete.setTagline(null);
        incomplete.setImageIds(List.of());
        var collection = collection(CollectionState.DRAFTED, complete, incomplete);
        when(collectionProvider.read(collectionId)).thenReturn(Optional.of(collection));
        when(jewelProvider.read(complete.getId())).thenReturn(Optional.of(complete));
        when(jewelProvider.read(incomplete.getId())).thenReturn(Optional.of(incomplete));

        var e = assertThrows(PublishCollection.InvalidCommand.class, this::handle);

        assertTrue(e.getMessage().contains("« Lune »"));
        assertTrue(e.getMessage().contains("une accroche"));
        assertTrue(e.getMessage().contains("au moins une image"));
        verify(jewelProvider, never()).write(any());
        verify(collectionProvider, never()).write(any());
    }

    @Test
    void handle_ignoresAnIncompleteArchivedJewel() {
        var archived = jewel("j1", JewelState.ARCHIVED);
        archived.setPrice(null);
        var collection = collection(CollectionState.DRAFTED, archived);
        when(collectionProvider.read(collectionId)).thenReturn(Optional.of(collection));
        when(jewelProvider.read(archived.getId())).thenReturn(Optional.of(archived));
        when(collectionProvider.write(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(CollectionState.PUBLISHED, handle().collection().getState());
    }

    @Test
    void handle_refusesACollectionWithoutVideo() {
        var collection = collection(CollectionState.DRAFTED);
        collection.setVideoId(null);
        when(collectionProvider.read(collectionId)).thenReturn(Optional.of(collection));

        assertThrows(PublishCollection.InvalidCommand.class, this::handle);
        verify(collectionProvider, never()).write(any());
    }

    @Test
    void handle_refusesACollectionWithoutPoster() {
        var collection = collection(CollectionState.DRAFTED);
        collection.setPosterImageId(null);
        when(collectionProvider.read(collectionId)).thenReturn(Optional.of(collection));

        assertThrows(PublishCollection.InvalidCommand.class, this::handle);
    }

    @Test
    void handle_refusesACollectionAlreadyPublished() {
        when(collectionProvider.read(collectionId)).thenReturn(Optional.of(collection(CollectionState.PUBLISHED)));

        assertThrows(PublishCollection.InvalidCommand.class, this::handle);
        verify(collectionProvider, never()).write(any());
    }

    @Test
    void handle_throwsWhenTheCollectionDoesNotExist() {
        when(collectionProvider.read(collectionId)).thenReturn(Optional.empty());

        assertThrows(PublishCollection.InvalidCommand.class, this::handle);
    }

    private PublishCollection.Result handle() {
        return new PublishCollection(collectionProvider, jewelProvider, clock).handle(new PublishCollection.Command(collectionId));
    }

    private Collection collection(CollectionState state, Jewel... jewels) {
        var collection = new Collection();
        collection.setId(collectionId);
        collection.setState(state);
        collection.setVideoId(new VideoId("v1"));
        collection.setPosterImageId(new ImageId("i1"));
        collection.setJewelIds(Arrays.stream(jewels).map(Jewel::getId).toList());
        return collection;
    }

    private static Jewel jewel(String id, JewelState state) {
        var jewel = new Jewel();
        jewel.setId(new JewelId(id));
        jewel.setState(state);
        jewel.setName("Bijou " + id);
        jewel.setCategory(JewelCategory.NECKLACE);
        jewel.setTagline("Une accroche");
        jewel.setPrice(new BigDecimal("42"));
        jewel.setImageIds(List.of(new ImageId("img-" + id)));
        return jewel;
    }

}
