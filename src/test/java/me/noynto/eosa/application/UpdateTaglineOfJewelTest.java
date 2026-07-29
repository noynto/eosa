package me.noynto.eosa.application;

import me.noynto.eosa.jewel.Jewel;
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
class UpdateTaglineOfJewelTest {

    @Mock JewelProvider jewelProvider;

    @Test
    void handle_updatesTaglineAndReturnsJewel() {
        var jewelId = new JewelId("abc");
        var jewel = jewelWith(jewelId);
        when(jewelProvider.read(jewelId)).thenReturn(Optional.of(jewel));
        when(jewelProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = new UpdateTaglineOfJewel(jewelProvider).handle(
                new UpdateTaglineOfJewel.Command(jewelId, "Bijou élégant")
        );

        assertEquals("Bijou élégant", result.getTagline());
    }

    @Test
    void handle_delegatesWriteWithUpdatedTagline() {
        var jewelId = new JewelId("abc");
        when(jewelProvider.read(jewelId)).thenReturn(Optional.of(jewelWith(jewelId)));
        when(jewelProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        new UpdateTaglineOfJewel(jewelProvider).handle(
                new UpdateTaglineOfJewel.Command(jewelId, "Bijou élégant")
        );

        verify(jewelProvider).write(argThat(p -> "Bijou élégant".equals(p.getTagline())));
    }

    @Test
    void handle_throwsWhenTaglineIsNull() {
        assertThrows(RuntimeException.class, () ->
                new UpdateTaglineOfJewel(jewelProvider).handle(
                        new UpdateTaglineOfJewel.Command(new JewelId("abc"), null)
                )
        );
    }

    @Test
    void handle_throwsWhenTaglineIsBlank() {
        assertThrows(RuntimeException.class, () ->
                new UpdateTaglineOfJewel(jewelProvider).handle(
                        new UpdateTaglineOfJewel.Command(new JewelId("abc"), "  ")
                )
        );
    }

    @Test
    void handle_throwsWhenJewelIdIsNull() {
        assertThrows(RuntimeException.class, () ->
                new UpdateTaglineOfJewel(jewelProvider).handle(
                        new UpdateTaglineOfJewel.Command(null, "Bijou élégant")
                )
        );
    }

    @Test
    void handle_throwsWhenJewelNotFound() {
        var jewelId = new JewelId("unknown");
        when(jewelProvider.read(jewelId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new UpdateTaglineOfJewel(jewelProvider).handle(
                        new UpdateTaglineOfJewel.Command(jewelId, "Bijou élégant")
                )
        );
    }

    private Jewel jewelWith(JewelId id) {
        var jewel = new Jewel();
        jewel.setId(id);
        return jewel;
    }

}