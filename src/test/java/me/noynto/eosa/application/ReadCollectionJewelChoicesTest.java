package me.noynto.eosa.application;

import me.noynto.eosa.collection.Collection;
import me.noynto.eosa.collection.CollectionProvider;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadCollectionJewelChoicesTest {

    @Mock CollectionProvider collectionProvider;
    @Mock JewelProvider jewelProvider;

    @Test
    void handle_marksTheJewelsOfTheCollection() {
        var collectionId = new CollectionId("c1");
        var in = new JewelId("in");
        var out = new JewelId("out");
        var collection = new Collection();
        collection.setId(collectionId);
        collection.setJewelIds(List.of(in));
        when(collectionProvider.read(collectionId)).thenReturn(Optional.of(collection));
        when(jewelProvider.readIds(null, null)).thenReturn(Stream.of(in, out));
        when(jewelProvider.read(in)).thenReturn(Optional.of(jewel(in)));
        when(jewelProvider.read(out)).thenReturn(Optional.of(jewel(out)));

        var choices = new ReadCollectionJewelChoices(collectionProvider, jewelProvider)
                .handle(new ReadCollectionJewelChoices.Command(collectionId)).choices();

        assertEquals(2, choices.size());
        assertEquals(true, choices.get(0).selected());
        assertEquals(false, choices.get(1).selected());
    }

    @Test
    void handle_throwsWhenTheCollectionDoesNotExist() {
        var unknown = new CollectionId("unknown");
        when(collectionProvider.read(unknown)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> new ReadCollectionJewelChoices(collectionProvider, jewelProvider)
                .handle(new ReadCollectionJewelChoices.Command(unknown)));
    }

    private static Jewel jewel(JewelId id) {
        var jewel = new Jewel();
        jewel.setId(id);
        return jewel;
    }

}
