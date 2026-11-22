package me.noynto.eosa.application;

import me.noynto.eosa.charm.Charm;
import me.noynto.eosa.charm.CharmProvider;
import me.noynto.eosa.jewel.Jewel;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.metal.MetalColor;
import me.noynto.eosa.metal.MetalColorProvider;
import me.noynto.eosa.shared.CharmId;
import me.noynto.eosa.shared.JewelId;
import me.noynto.eosa.shared.MetalColorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadJewelSelectionTest {

    private static final JewelId JEWEL_ID = new JewelId("jewel1");

    @Mock JewelProvider jewelProvider;
    @Mock MetalColorProvider metalColorProvider;
    @Mock CharmProvider charmProvider;

    private ReadJewelSelection readJewelSelection;

    @BeforeEach
    void setUp() {
        readJewelSelection = new ReadJewelSelection(jewelProvider, metalColorProvider, charmProvider);
        lenient().when(jewelProvider.read(JEWEL_ID)).thenReturn(Optional.of(jewel("42")));
    }

    @Test
    void handle_withoutChoice_takesTheFirstMetalColorAndTheJewelPrice() {
        var silver = metalColor("m1", "Argent");
        when(metalColorProvider.readIds()).thenReturn(Stream.of(silver.getId()));
        when(metalColorProvider.read(silver.getId())).thenReturn(Optional.of(silver));

        var result = readJewelSelection.handle(new ReadJewelSelection.Command(JEWEL_ID, null, List.of()));

        assertEquals("Argent", result.metalColorName());
        assertEquals(List.of(), result.charmNames());
        assertEquals(new BigDecimal("42"), result.total());
    }

    @Test
    void handle_withChoices_addsTheCharmPricesToTheJewelPrice() {
        var gold = metalColor("m2", "Or jaune");
        var moon = charm("c1", "Lune", "6.50");
        var star = charm("c2", "Étoile", "5");
        when(metalColorProvider.read(gold.getId())).thenReturn(Optional.of(gold));
        when(charmProvider.read(moon.getId())).thenReturn(Optional.of(moon));
        when(charmProvider.read(star.getId())).thenReturn(Optional.of(star));

        var result = readJewelSelection.handle(
                new ReadJewelSelection.Command(JEWEL_ID, gold.getId(), List.of(moon.getId(), star.getId())));

        assertEquals("Or jaune", result.metalColorName());
        assertEquals(List.of("Lune", "Étoile"), result.charmNames());
        assertEquals(new BigDecimal("53.50"), result.total());
    }

    @Test
    void handle_ignoresAnUnknownCharm() {
        when(metalColorProvider.readIds()).thenReturn(Stream.empty());
        var unknown = new CharmId("gone");
        when(charmProvider.read(unknown)).thenReturn(Optional.empty());

        var result = readJewelSelection.handle(new ReadJewelSelection.Command(JEWEL_ID, null, List.of(unknown)));

        assertNull(result.metalColorName());
        assertEquals(List.of(), result.charmNames());
        assertEquals(new BigDecimal("42"), result.total());
    }

    @Test
    void handle_refusesAnUnknownJewel() {
        var missing = new JewelId("missing");
        when(jewelProvider.read(missing)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> readJewelSelection.handle(new ReadJewelSelection.Command(missing, null, List.of())));
    }

    private Jewel jewel(String price) {
        var jewel = new Jewel();
        jewel.setId(JEWEL_ID);
        jewel.setPrice(new BigDecimal(price));
        return jewel;
    }

    private MetalColor metalColor(String id, String name) {
        var metalColor = new MetalColor();
        metalColor.setId(new MetalColorId(id));
        metalColor.setName(name);
        return metalColor;
    }

    private Charm charm(String id, String name, String price) {
        var charm = new Charm();
        charm.setId(new CharmId(id));
        charm.setName(name);
        charm.setPrice(new BigDecimal(price));
        return charm;
    }

}
