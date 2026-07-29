package me.noynto.eosa.application;

import me.noynto.eosa.jewel.Jewel;
import me.noynto.eosa.jewel.JewelCategory;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.shared.JewelId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateCategoryOfJewelTest {

    @Mock JewelProvider jewelProvider;

    @Test
    void handle_updatesCategoryAndReturnsJewel() {
        var jewelId = new JewelId("abc");
        when(jewelProvider.read(jewelId)).thenReturn(Optional.of(jewelWith(jewelId)));
        when(jewelProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = new UpdateCategoryOfJewel(jewelProvider).handle(
                new UpdateCategoryOfJewel.Command(jewelId, JewelCategory.NECKLACE)
        );

        assertEquals(JewelCategory.NECKLACE, result.getCategory());
    }

    @Test
    void handle_delegatesWriteWithUpdatedCategory() {
        var jewelId = new JewelId("abc");
        when(jewelProvider.read(jewelId)).thenReturn(Optional.of(jewelWith(jewelId)));
        when(jewelProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        new UpdateCategoryOfJewel(jewelProvider).handle(
                new UpdateCategoryOfJewel.Command(jewelId, JewelCategory.BRACELET)
        );

        verify(jewelProvider).write(argThat(p -> JewelCategory.BRACELET == p.getCategory()));
    }

    @Test
    void handle_throwsWhenCategoryIsNull() {
        assertThrows(RuntimeException.class, () ->
                new UpdateCategoryOfJewel(jewelProvider).handle(
                        new UpdateCategoryOfJewel.Command(new JewelId("abc"), null)
                )
        );
    }

    @Test
    void handle_throwsWhenJewelIdIsNull() {
        assertThrows(RuntimeException.class, () ->
                new UpdateCategoryOfJewel(jewelProvider).handle(
                        new UpdateCategoryOfJewel.Command(null, JewelCategory.NECKLACE)
                )
        );
    }

    @Test
    void handle_throwsWhenJewelNotFound() {
        var jewelId = new JewelId("unknown");
        when(jewelProvider.read(jewelId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new UpdateCategoryOfJewel(jewelProvider).handle(
                        new UpdateCategoryOfJewel.Command(jewelId, JewelCategory.NECKLACE)
                )
        );
    }

    private Jewel jewelWith(JewelId id) {
        var jewel = new Jewel();
        jewel.setId(id);
        return jewel;
    }

}