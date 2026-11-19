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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadHomeTest {

    @Mock JewelProvider jewelProvider;
    @Mock CollectionProvider collectionProvider;

    @Test
    void handle_countsPublishedJewelsAndFindsTheLowestPricePerCategory() {
        publishedJewels(JewelCategory.NECKLACE, jewel("n1", "89.00"), jewel("n2", "49.50"));
        publishedJewels(JewelCategory.BRACELET, jewel("b1", "35.00"));

        var result = handle(List.of(JewelCategory.NECKLACE, JewelCategory.BRACELET));

        assertEquals(List.of(
                new ReadHome.Stats(JewelCategory.NECKLACE, 2, new BigDecimal("49.50")),
                new ReadHome.Stats(JewelCategory.BRACELET, 1, new BigDecimal("35.00"))
        ), result.stats());
    }

    @Test
    void handle_hasNoPriceForACategoryWithoutPublishedJewel() {
        publishedJewels(JewelCategory.NECKLACE);

        var result = handle(List.of(JewelCategory.NECKLACE));

        assertEquals(0, result.stats().getFirst().count());
        assertNull(result.stats().getFirst().minPrice());
    }

    @Test
    void handle_takesTheHeroFromTheMostRecentPublishedCollection() {
        publishedJewels(JewelCategory.NECKLACE);
        publishedCollections(collection("c-new", "Été 2027", "v-new", "p-new"), collection("c-old", "Hiver 2026", "v-old", "p-old"));

        var hero = handle(List.of(JewelCategory.NECKLACE)).hero().orElseThrow();

        assertEquals("Été 2027", hero.collectionName());
        assertEquals(new VideoId("v-new"), hero.videoId());
        assertEquals(new ImageId("p-new"), hero.posterImageId());
    }

    @Test
    void handle_skipsAPublishedCollectionWithoutItsMedia() {
        publishedJewels(JewelCategory.NECKLACE);
        publishedCollections(collection("c-new", "Été 2027", null, "p-new"), collection("c-old", "Hiver 2026", "v-old", "p-old"));

        var hero = handle(List.of(JewelCategory.NECKLACE)).hero().orElseThrow();

        assertEquals("Hiver 2026", hero.collectionName());
    }

    @Test
    void handle_hasNoHeroWhenNoCollectionIsPublished() {
        publishedJewels(JewelCategory.NECKLACE);
        publishedCollections();

        assertTrue(handle(List.of(JewelCategory.NECKLACE)).hero().isEmpty());
    }

    private ReadHome.Result handle(List<JewelCategory> categories) {
        return new ReadHome(jewelProvider, collectionProvider).handle(new ReadHome.Query(categories));
    }

    private void publishedJewels(JewelCategory category, Jewel... jewels) {
        when(jewelProvider.readIds(Set.of(JewelState.PUBLISHED), Set.of(category)))
                .thenReturn(Stream.of(jewels).map(Jewel::getId));
        for (Jewel jewel : jewels) {
            when(jewelProvider.read(jewel.getId())).thenReturn(Optional.of(jewel));
        }
    }

    /** Dans l'ordre rendu par le provider : la plus récemment publiée d'abord. */
    private void publishedCollections(Collection... collections) {
        when(collectionProvider.readIds(Set.of(CollectionState.PUBLISHED)))
                .thenReturn(Stream.of(collections).map(Collection::getId));
        for (Collection collection : collections) {
            lenient().when(collectionProvider.read(collection.getId())).thenReturn(Optional.of(collection));
        }
    }

    private static Collection collection(String id, String name, String videoId, String posterId) {
        var collection = new Collection();
        collection.setId(new CollectionId(id));
        collection.setName(name);
        collection.setState(CollectionState.PUBLISHED);
        collection.setVideoId(videoId == null ? null : new VideoId(videoId));
        collection.setPosterImageId(posterId == null ? null : new ImageId(posterId));
        return collection;
    }

    private static Jewel jewel(String id, String price) {
        var jewel = new Jewel();
        jewel.setId(new JewelId(id));
        jewel.setPrice(new BigDecimal(price));
        return jewel;
    }

}
