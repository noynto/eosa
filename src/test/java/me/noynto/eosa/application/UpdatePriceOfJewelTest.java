package me.noynto.eosa.application;

import me.noynto.eosa.jewel.Jewel;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.shared.JewelId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdatePriceOfJewelTest {

    @Mock JewelProvider jewelProvider;

    @Test
    void handle_updatesPriceAndReturnsJewel() {
        var jewelId = new JewelId("abc");
        when(jewelProvider.read(jewelId)).thenReturn(Optional.of(jewelWith(jewelId)));
        when(jewelProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = new UpdatePriceOfJewel(jewelProvider).handle(
                new UpdatePriceOfJewel.Command(jewelId, new BigDecimal("29.90"))
        );

        assertEquals(new BigDecimal("29.90"), result.getPrice());
    }

    @Test
    void handle_delegatesWriteWithUpdatedPrice() {
        var jewelId = new JewelId("abc");
        when(jewelProvider.read(jewelId)).thenReturn(Optional.of(jewelWith(jewelId)));
        when(jewelProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        new UpdatePriceOfJewel(jewelProvider).handle(
                new UpdatePriceOfJewel.Command(jewelId, new BigDecimal("49.99"))
        );

        verify(jewelProvider).write(argThat(p -> new BigDecimal("49.99").equals(p.getPrice())));
    }

    @Test
    void handle_throwsWhenPriceIsNull() {
        assertThrows(RuntimeException.class, () ->
                new UpdatePriceOfJewel(jewelProvider).handle(
                        new UpdatePriceOfJewel.Command(new JewelId("abc"), null)
                )
        );
    }

    @Test
    void handle_throwsWhenPriceIsZero() {
        assertThrows(RuntimeException.class, () ->
                new UpdatePriceOfJewel(jewelProvider).handle(
                        new UpdatePriceOfJewel.Command(new JewelId("abc"), BigDecimal.ZERO)
                )
        );
    }

    @Test
    void handle_throwsWhenPriceIsNegative() {
        assertThrows(RuntimeException.class, () ->
                new UpdatePriceOfJewel(jewelProvider).handle(
                        new UpdatePriceOfJewel.Command(new JewelId("abc"), new BigDecimal("-5"))
                )
        );
    }

    @Test
    void handle_throwsWhenJewelIdIsNull() {
        assertThrows(RuntimeException.class, () ->
                new UpdatePriceOfJewel(jewelProvider).handle(
                        new UpdatePriceOfJewel.Command(null, new BigDecimal("10"))
                )
        );
    }

    @Test
    void handle_throwsWhenJewelNotFound() {
        var jewelId = new JewelId("unknown");
        when(jewelProvider.read(jewelId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                new UpdatePriceOfJewel(jewelProvider).handle(
                        new UpdatePriceOfJewel.Command(jewelId, new BigDecimal("10"))
                )
        );
    }

    private Jewel jewelWith(JewelId id) {
        var jewel = new Jewel();
        jewel.setId(id);
        return jewel;
    }

}