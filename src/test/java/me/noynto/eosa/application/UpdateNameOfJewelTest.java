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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateNameOfJewelTest {

    @Mock JewelProvider jewelProvider;

    @Test
    void handle_renamesTheJewelAndReturnsIt() {
        var jewelId = new JewelId("abc");
        when(jewelProvider.read(jewelId)).thenReturn(Optional.of(jewelWith(jewelId, "Lune")));
        when(jewelProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = new UpdateNameOfJewel(jewelProvider).handle(
                new UpdateNameOfJewel.Command(jewelId, "Étoile")
        ).jewel();

        assertEquals("Étoile", result.getName());
    }

    @Test
    void handle_writesTheNameWithoutSurroundingSpaces() {
        var jewelId = new JewelId("abc");
        when(jewelProvider.read(jewelId)).thenReturn(Optional.of(jewelWith(jewelId, "Lune")));
        when(jewelProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        new UpdateNameOfJewel(jewelProvider).handle(new UpdateNameOfJewel.Command(jewelId, "  Étoile  "));

        verify(jewelProvider).write(argThat(jewel -> "Étoile".equals(jewel.getName())));
    }

    @Test
    void handle_refusesANullName() {
        assertThrows(RuntimeException.class, () ->
                new UpdateNameOfJewel(jewelProvider).handle(new UpdateNameOfJewel.Command(new JewelId("abc"), null)));
    }

    @Test
    void handle_refusesABlankNameAndWritesNothing() {
        assertThrows(RuntimeException.class, () ->
                new UpdateNameOfJewel(jewelProvider).handle(new UpdateNameOfJewel.Command(new JewelId("abc"), "  ")));

        verify(jewelProvider, never()).write(any());
    }

    @Test
    void handle_refusesAMissingJewelId() {
        assertThrows(RuntimeException.class, () ->
                new UpdateNameOfJewel(jewelProvider).handle(new UpdateNameOfJewel.Command(null, "Étoile")));
    }

    @Test
    void handle_refusesAnUnknownJewel() {
        var jewelId = new JewelId("missing");
        when(jewelProvider.read(jewelId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new UpdateNameOfJewel(jewelProvider).handle(new UpdateNameOfJewel.Command(jewelId, "Étoile")));
    }

    private Jewel jewelWith(JewelId id, String name) {
        var jewel = new Jewel();
        jewel.setId(id);
        jewel.setName(name);
        return jewel;
    }

}
