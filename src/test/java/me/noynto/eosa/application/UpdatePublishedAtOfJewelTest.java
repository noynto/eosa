package me.noynto.eosa.application;

import me.noynto.eosa.jewel.Jewel;
import me.noynto.eosa.jewel.JewelProvider;
import me.noynto.eosa.shared.JewelId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdatePublishedAtOfJewelTest {

    @Mock JewelProvider jewelProvider;

    // 16 novembre 2026, 00:30 à Paris (UTC+1) : le 15 novembre à 23:30 en UTC.
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-15T23:30:00Z"), ZoneOffset.UTC);
    private final JewelId jewelId = new JewelId("abc");

    private UpdatePublishedAtOfJewel useCase() {
        return new UpdatePublishedAtOfJewel(jewelProvider, clock);
    }

    private Jewel published() {
        var jewel = new Jewel();
        jewel.setId(jewelId);
        jewel.setPublishedAt(Instant.parse("2026-11-10T12:00:00Z"));
        return jewel;
    }

    @Test
    void handle_setsTheDayAtMidnightInParis() {
        when(jewelProvider.read(jewelId)).thenReturn(Optional.of(published()));
        when(jewelProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase().handle(new UpdatePublishedAtOfJewel.Command(jewelId, LocalDate.of(2026, 11, 3))).jewel();

        // Novembre : Paris est en UTC+1, minuit à Paris est 23:00 la veille en UTC.
        assertEquals(Instant.parse("2026-11-02T23:00:00Z"), result.getPublishedAt());
    }

    @Test
    void handle_acceptsTodayInParisEvenWhenItIsYesterdayInUtc() {
        when(jewelProvider.read(jewelId)).thenReturn(Optional.of(published()));
        when(jewelProvider.write(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase().handle(new UpdatePublishedAtOfJewel.Command(jewelId, LocalDate.of(2026, 11, 16))).jewel();

        assertEquals(Instant.parse("2026-11-15T23:00:00Z"), result.getPublishedAt());
    }

    @Test
    void handle_refusesAFutureDate() {
        var error = assertThrows(RuntimeException.class,
                () -> useCase().handle(new UpdatePublishedAtOfJewel.Command(jewelId, LocalDate.of(2026, 11, 17))));

        assertEquals("La date de publication ne peut pas être dans le futur.", error.getMessage());
        verify(jewelProvider, never()).write(any());
    }

    @Test
    void handle_refusesAJewelThatWasNeverPublished() {
        var draft = new Jewel();
        draft.setId(jewelId);
        when(jewelProvider.read(jewelId)).thenReturn(Optional.of(draft));

        assertThrows(RuntimeException.class,
                () -> useCase().handle(new UpdatePublishedAtOfJewel.Command(jewelId, LocalDate.of(2026, 11, 3))));
        verify(jewelProvider, never()).write(any());
    }

    @Test
    void handle_throwsWhenIdOrDateIsMissingOrJewelNotFound() {
        assertThrows(RuntimeException.class,
                () -> useCase().handle(new UpdatePublishedAtOfJewel.Command(null, LocalDate.of(2026, 11, 3))));
        assertThrows(RuntimeException.class,
                () -> useCase().handle(new UpdatePublishedAtOfJewel.Command(jewelId, null)));
        when(jewelProvider.read(jewelId)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,
                () -> useCase().handle(new UpdatePublishedAtOfJewel.Command(jewelId, LocalDate.of(2026, 11, 3))));
    }

}
