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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadJewelTest {

    @Mock JewelProvider jewelProvider;

    @Test
    void handle_returnsJewelFromProvider() {
        var jewelId = new JewelId("abc");
        var expected = new Jewel();
        expected.setId(jewelId);
        when(jewelProvider.read(jewelId)).thenReturn(Optional.of(expected));

        var result = new ReadJewel(jewelProvider).handle(new ReadJewel.Command(jewelId));

        assertEquals(expected, result);
    }

    @Test
    void handle_throwsWhenJewelNotFound() {
        var jewelId = new JewelId("unknown");
        when(jewelProvider.read(jewelId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new ReadJewel(jewelProvider).handle(new ReadJewel.Command(jewelId))
        );
    }

}