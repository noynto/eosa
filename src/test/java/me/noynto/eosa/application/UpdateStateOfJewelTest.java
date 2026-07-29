
package me.noynto.eosa.application;

import me.noynto.eosa.jewel.Jewel;
import me.noynto.eosa.jewel.JewelCategory;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.jewel.JewelState;
import me.noynto.eosa.shared.ImageId;
import me.noynto.eosa.shared.JewelId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateStateOfJewelTest {

    @Mock JewelProvider jewelProvider;

    @Test
    void handle_publishesCompleteJewel() {
        var jewelId = new JewelId("abc");
        var jewel = completeJewel(jewelId, JewelState.DRAFTED);
        when(jewelProvider.read(jewelId)).thenReturn(Optional.of(jewel));
        when(jewelProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = new UpdateStateOfJewel(jewelProvider).handle(
                new UpdateStateOfJewel.Command(jewelId, JewelState.PUBLISHED)
        );

        assertEquals(JewelState.PUBLISHED, result.getState());
    }

    @Test
    void handle_archivesPublishedJewel() {
        var jewelId = new JewelId("abc");
        var jewel = completeJewel(jewelId, JewelState.PUBLISHED);
        when(jewelProvider.read(jewelId)).thenReturn(Optional.of(jewel));
        when(jewelProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = new UpdateStateOfJewel(jewelProvider).handle(
                new UpdateStateOfJewel.Command(jewelId, JewelState.ARCHIVED)
        );

        assertEquals(JewelState.ARCHIVED, result.getState());
    }

    @Test
    void handle_throwsWhenStateIsNull() {
        assertThrows(RuntimeException.class, () ->
                new UpdateStateOfJewel(jewelProvider).handle(
                        new UpdateStateOfJewel.Command(new JewelId("abc"), null)
                )
        );
    }

    @Test
    void handle_throwsWhenJewelIdIsNull() {
        assertThrows(RuntimeException.class, () ->
                new UpdateStateOfJewel(jewelProvider).handle(
                        new UpdateStateOfJewel.Command(null, JewelState.PUBLISHED)
                )
        );
    }

    @Test
    void handle_throwsWhenJewelNotFound() {
        var jewelId = new JewelId("unknown");
        when(jewelProvider.read(jewelId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new UpdateStateOfJewel(jewelProvider).handle(
                        new UpdateStateOfJewel.Command(jewelId, JewelState.PUBLISHED)
                )
        );
    }

    @Test
    void handle_throwsWhenAlreadyInSameState() {
        var jewelId = new JewelId("abc");
        var jewel = completeJewel(jewelId, JewelState.DRAFTED);
        when(jewelProvider.read(jewelId)).thenReturn(Optional.of(jewel));

        assertThrows(RuntimeException.class, () ->
                new UpdateStateOfJewel(jewelProvider).handle(
                        new UpdateStateOfJewel.Command(jewelId, JewelState.DRAFTED)
                )
        );
    }

    @Test
    void handle_throwsWhenPublishedToDrafted() {
        var jewelId = new JewelId("abc");
        var jewel = completeJewel(jewelId, JewelState.PUBLISHED);
        when(jewelProvider.read(jewelId)).thenReturn(Optional.of(jewel));

        assertThrows(RuntimeException.class, () ->
                new UpdateStateOfJewel(jewelProvider).handle(
                        new UpdateStateOfJewel.Command(jewelId, JewelState.DRAFTED)
                )
        );
    }

    @Test
    void handle_throwsWhenPublishingWithoutName() {
        var jewelId = new JewelId("abc");
        var jewel = completeJewel(jewelId, JewelState.DRAFTED);
        jewel.setName(null);
        when(jewelProvider.read(jewelId)).thenReturn(Optional.of(jewel));

        assertThrows(RuntimeException.class, () ->
                new UpdateStateOfJewel(jewelProvider).handle(
                        new UpdateStateOfJewel.Command(jewelId, JewelState.PUBLISHED)
                )
        );
    }

    @Test
    void handle_throwsWhenPublishingWithoutCategory() {
        var jewelId = new JewelId("abc");
        var jewel = completeJewel(jewelId, JewelState.DRAFTED);
        jewel.setCategory(null);
        when(jewelProvider.read(jewelId)).thenReturn(Optional.of(jewel));

        assertThrows(RuntimeException.class, () ->
                new UpdateStateOfJewel(jewelProvider).handle(
                        new UpdateStateOfJewel.Command(jewelId, JewelState.PUBLISHED)
                )
        );
    }

    @Test
    void handle_throwsWhenPublishingWithoutTagline() {
        var jewelId = new JewelId("abc");
        var jewel = completeJewel(jewelId, JewelState.DRAFTED);
        jewel.setTagline(null);
        when(jewelProvider.read(jewelId)).thenReturn(Optional.of(jewel));

        assertThrows(RuntimeException.class, () ->
                new UpdateStateOfJewel(jewelProvider).handle(
                        new UpdateStateOfJewel.Command(jewelId, JewelState.PUBLISHED)
                )
        );
    }

    @Test
    void handle_throwsWhenPublishingWithoutPrice() {
        var jewelId = new JewelId("abc");
        var jewel = completeJewel(jewelId, JewelState.DRAFTED);
        jewel.setPrice(null);
        when(jewelProvider.read(jewelId)).thenReturn(Optional.of(jewel));

        assertThrows(RuntimeException.class, () ->
                new UpdateStateOfJewel(jewelProvider).handle(
                        new UpdateStateOfJewel.Command(jewelId, JewelState.PUBLISHED)
                )
        );
    }

    @Test
    void handle_throwsWhenPublishingWithoutImages() {
        var jewelId = new JewelId("abc");
        var jewel = completeJewel(jewelId, JewelState.DRAFTED);
        jewel.setImageIds(List.of());
        when(jewelProvider.read(jewelId)).thenReturn(Optional.of(jewel));

        assertThrows(RuntimeException.class, () ->
                new UpdateStateOfJewel(jewelProvider).handle(
                        new UpdateStateOfJewel.Command(jewelId, JewelState.PUBLISHED)
                )
        );
    }

    private Jewel completeJewel(JewelId id, JewelState state) {
        var jewel = new Jewel();
        jewel.setId(id);
        jewel.setState(state);
        jewel.setName("Lune");
        jewel.setTagline("Bijou élégant");
        jewel.setCategory(JewelCategory.NECKLACE);
        jewel.setPrice(new BigDecimal("29.90"));
        jewel.setImageIds(List.of(new ImageId("img1")));
        return jewel;
    }

}